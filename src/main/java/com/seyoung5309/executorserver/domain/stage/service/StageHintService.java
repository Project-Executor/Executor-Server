package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.HintResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageHintRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StageHintService {

    private final StageAccessService stageAccessService;
    private final MapDataParser mapDataParser;
    private final UserItemRepository userItemRepository;
    private final UserStageHintRepository userStageHintRepository;
    private final ProfileRepository profileRepository;
    private final Clock clock;

    /**
     * 이 스테이지의 힌트 하나를 받는다. 처음 보는 힌트면 힌트 아이템 1개를 쓰고, 이미 본 힌트면 무료다.
     * <p>
     * 같은 사용자의 요청은 프로필 행을 잠가 하나씩 처리한다 (잠금 순서: profiles -> user_stage_hints -> user_items).
     * 아이템이 없어 실패하면 트랜잭션이 롤백되어 열람 기록도 남지 않는다.
     *
     * @throws BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN(남의 비공개 맵),
     *                           INVALID_INPUT(범위를 벗어난 hintIndex), NOT_ENOUGH_ITEM
     */
    @Transactional
    public HintResponse useHint(Long userId, Long stageId, int hintIndex) {
        Stage stage = stageAccessService.getPlayableStage(userId, stageId);

        // 범위 검사를 아이템 차감보다 먼저 해서, 잘못된 요청으로 아이템이 빠지지 않게 한다
        List<JsonNode> hints = mapDataParser.hints(stage.getMapData());
        if (hintIndex < 0 || hintIndex >= hints.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, hints.isEmpty()
                    ? "이 스테이지에는 힌트가 없습니다."
                    : "hintIndex는 0~" + (hints.size() - 1) + " 사이여야 합니다.");
        }

        profileRepository.lockOne(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        boolean firstView = userStageHintRepository.insertIfAbsent(userId, stageId, hintIndex, Instant.now(clock)) == 1;
        if (firstView && userItemRepository.useOneHint(userId) == 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_ITEM, "힌트 아이템이 없습니다. 상점에서 구매해 주세요.");
        }

        return new HintResponse(hintIndex, hints.get(hintIndex), firstView, userItemRepository.sumHintQuantity(userId));
    }
}
