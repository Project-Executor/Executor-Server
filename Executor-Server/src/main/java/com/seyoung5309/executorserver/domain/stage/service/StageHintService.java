package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.item.repository.UserItemRepository;
import com.seyoung5309.executorserver.domain.stage.dto.HintResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StageHintService {

    private final StageAccessService stageAccessService;
    private final MapDataParser mapDataParser;
    private final UserItemRepository userItemRepository;

    /**
     * 힌트 아이템 1개를 쓰고 이 스테이지의 힌트 하나를 받는다.
     * <p>
     * 어떤 힌트를 이미 봤는지는 저장하지 않으므로, 같은 힌트를 다시 요청해도 아이템이 1개 더 빠진다.
     * 받은 힌트는 클라이언트가 기억해 두어야 한다.
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

        if (userItemRepository.useOneHint(userId) == 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_ITEM, "힌트 아이템이 없습니다. 상점에서 구매해 주세요.");
        }

        return new HintResponse(hintIndex, hints.get(hintIndex), userItemRepository.sumHintQuantity(userId));
    }
}
