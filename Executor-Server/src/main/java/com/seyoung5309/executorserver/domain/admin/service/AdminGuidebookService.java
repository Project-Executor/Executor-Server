package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminGuidebookRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminGuidebookResponse;
import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminGuidebookService {

    private static final Long NEW = 0L;

    private final GuidebookRepository guidebookRepository;
    private final StageRepository stageRepository;

    /** @throws BusinessException NOT_FOUND(스테이지), DUPLICATE_VALUE(이름 중복) */
    public AdminGuidebookResponse create(AdminGuidebookRequest request) {
        String title = request.title().strip();
        Stage stage = findStage(request.stageId());
        checkDuplicate(NEW, title);

        Guidebook guidebook = new Guidebook(stage, request.category(), title,
                AdminText.blankToNull(request.image()), request.comment().strip());
        saveAndFlush(guidebook);
        return AdminGuidebookResponse.from(guidebook);
    }

    /** @throws BusinessException NOT_FOUND(도감 또는 스테이지), DUPLICATE_VALUE */
    public AdminGuidebookResponse update(Long guidebookId, AdminGuidebookRequest request) {
        Guidebook guidebook = find(guidebookId);
        String title = request.title().strip();
        Stage stage = findStage(request.stageId());
        checkDuplicate(guidebookId, title);

        guidebook.update(stage, request.category(), title,
                AdminText.blankToNull(request.image()), request.comment().strip());
        saveAndFlush(guidebook);
        return AdminGuidebookResponse.from(guidebook);
    }

    /**
     * 삭제한다. 사용자들의 해금 기록(user_guidebooks)은 DB에서 함께 지워진다(CASCADE).
     *
     * @throws BusinessException NOT_FOUND
     */
    public void delete(Long guidebookId) {
        guidebookRepository.delete(find(guidebookId));
        guidebookRepository.flush();
    }

    private Guidebook find(Long guidebookId) {
        return guidebookRepository.findById(guidebookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "도감 항목을 찾을 수 없습니다."));
    }

    /** stageId가 없으면 null(처음 등장 스테이지 없음) */
    private Stage findStage(Long stageId) {
        if (stageId == null) {
            return null;
        }
        return stageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "스테이지를 찾을 수 없습니다."));
    }

    private void checkDuplicate(Long selfId, String title) {
        if (guidebookRepository.existsByTitleAndIdNot(title, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 도감 이름입니다.");
        }
    }

    private void saveAndFlush(Guidebook guidebook) {
        try {
            guidebookRepository.saveAndFlush(guidebook);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 도감 이름입니다.");
        }
    }
}
