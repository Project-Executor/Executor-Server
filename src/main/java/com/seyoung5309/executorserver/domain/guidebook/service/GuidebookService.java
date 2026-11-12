package com.seyoung5309.executorserver.domain.guidebook.service;

import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookDetailResponse;
import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookListResponse;
import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import com.seyoung5309.executorserver.domain.guidebook.entity.UserGuidebook;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UnlockedGuidebookRow;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuidebookService {

    private final GuidebookRepository guidebookRepository;
    private final UserGuidebookRepository userGuidebookRepository;

    /**
     * 도감 목록 (도감 ID 순).
     *
     * @param category null이면 전체
     */
    public GuidebookListResponse getGuidebooks(Long userId, GuidebookCategory category) {
        List<Guidebook> guidebooks = category == null
                ? guidebookRepository.findAllByOrderByIdAsc()
                : guidebookRepository.findAllByCategoryOrderByIdAsc(category);
        Map<Long, Instant> unlockedAt = userGuidebookRepository.findUnlockedByUserId(userId).stream()
                .collect(Collectors.toMap(UnlockedGuidebookRow::guidebookId, UnlockedGuidebookRow::unlockedAt));

        List<GuidebookListResponse.Item> items = guidebooks.stream()
                .map(guidebook -> GuidebookListResponse.Item.of(guidebook, unlockedAt.get(guidebook.getId())))
                .toList();
        long unlockedCount = items.stream().filter(GuidebookListResponse.Item::unlocked).count();

        return new GuidebookListResponse(unlockedCount, items.size(), items);
    }

    /**
     * 해금한 도감 항목의 상세.
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN(아직 해금하지 않음)
     */
    public GuidebookDetailResponse getGuidebook(Long userId, Long guidebookId) {
        Guidebook guidebook = guidebookRepository.findById(guidebookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "도감 항목을 찾을 수 없습니다."));
        UserGuidebook unlocked = userGuidebookRepository.findByUserIdAndGuidebookId(userId, guidebookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "아직 해금하지 않은 도감 항목입니다."));

        return new GuidebookDetailResponse(
                guidebook.getId(),
                guidebook.getCategory(),
                guidebook.getTitle(),
                guidebook.getImage(),
                guidebook.getComment(),
                guidebook.getStage() == null ? null : guidebook.getStage().getId(),
                unlocked.getUnlockedAt()
        );
    }

    /** 분류 문자열을 읽는다 (대소문자 무시). 비어 있으면 null(전체), 잘못된 값이면 INVALID_INPUT */
    public static GuidebookCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return GuidebookCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "category는 BUG, VIRUS, TERM 중 하나여야 합니다.");
        }
    }
}
