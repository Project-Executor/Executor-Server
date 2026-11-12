package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 스테이지를 플레이(조회·결과 제출·힌트 사용)할 수 있는지 확인한다.
 * <ul>
 *   <li>공식 스테이지: 해금되어 있어야 한다 -> 아니면 STAGE_LOCKED</li>
 *   <li>사용자 맵: 공개됐거나 내가 만든 맵이어야 한다 -> 아니면 FORBIDDEN</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageAccessService {

    private final StageRepository stageRepository;
    private final UserStageRepository userStageRepository;

    /**
     * @throws BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN
     */
    public Stage getPlayableStage(Long userId, Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "스테이지를 찾을 수 없습니다."));

        if (stage.isOfficial()) {
            if (!isUnlocked(userId, stage)) {
                throw new BusinessException(ErrorCode.STAGE_LOCKED);
            }
        } else if (!stage.isPublic() && !Objects.equals(stage.getCreator().getId(), userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "비공개 맵입니다.");
        }
        return stage;
    }

    private boolean isUnlocked(Long userId, Stage stage) {
        return StageUnlockPolicy.unlockedStageIds(
                stageRepository.findAllOfficialInPlayOrder(),
                userStageRepository.findClearedOfficialStageIds(userId)
        ).contains(stage.getId());
    }
}
