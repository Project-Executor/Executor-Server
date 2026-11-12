package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminStageRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminStageResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 공식 스테이지 관리. 사용자 맵(creator_id가 있는 스테이지)은 여기서 다루지 않는다.
 * <p>
 * 해금 순서는 작전 sortOrder -> stageNo로 정해지므로, 번호를 바꾸면 사용자들의 해금 상태도 그에 맞게 다시 계산된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminStageService {

    private static final Long NEW = 0L;

    private final StageRepository stageRepository;
    private final OperationRepository operationRepository;
    private final ObjectMapper objectMapper;

    /** 관리자용 상세 (hints 포함) */
    @Transactional(readOnly = true)
    public AdminStageResponse get(Long stageId) {
        return toResponse(findOfficial(stageId));
    }

    /** @throws BusinessException NOT_FOUND(작전), INVALID_INPUT(mapData), DUPLICATE_VALUE(같은 작전에 같은 번호) */
    public AdminStageResponse create(AdminStageRequest request) {
        Operation operation = findOperation(request.operationId());
        String mapData = toMapDataJson(request.mapData());
        checkDuplicate(NEW, operation.getId(), request.stageNo());

        Stage stage = Stage.official(operation, request.stageNo(), request.stageType(), request.title().strip(),
                mapData, request.parMoves(), request.rewardCoin());
        saveAndFlush(stage);
        return toResponse(stage);
    }

    /**
     * 내용을 바꾼다. 사용자들의 기록(클리어, 최고 기록)은 그대로 둔다.
     *
     * @throws BusinessException NOT_FOUND(스테이지 또는 작전), INVALID_INPUT, DUPLICATE_VALUE
     */
    public AdminStageResponse update(Long stageId, AdminStageRequest request) {
        Stage stage = findOfficial(stageId);
        Operation operation = findOperation(request.operationId());
        String mapData = toMapDataJson(request.mapData());
        checkDuplicate(stageId, operation.getId(), request.stageNo());

        stage.updateOfficial(operation, request.stageNo(), request.stageType(), request.title().strip(),
                mapData, request.parMoves(), request.rewardCoin());
        saveAndFlush(stage);
        return toResponse(stage);
    }

    /**
     * 삭제한다. 사용자 기록(user_stages)은 DB에서 함께 지워지고(CASCADE), 이 스테이지를 가리키던 도감의 stageId는 NULL이 된다(SET NULL).
     *
     * @throws BusinessException NOT_FOUND
     */
    public void delete(Long stageId) {
        stageRepository.delete(findOfficial(stageId));
        stageRepository.flush();
    }

    private Stage findOfficial(Long stageId) {
        return stageRepository.findById(stageId)
                .filter(Stage::isOfficial)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공식 스테이지를 찾을 수 없습니다."));
    }

    private Operation findOperation(Long operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "작전을 찾을 수 없습니다."));
    }

    /** mapData는 JSON 객체여야 하고, hints가 있으면 배열이어야 한다 */
    private String toMapDataJson(JsonNode mapData) {
        if (mapData == null || !mapData.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mapData는 JSON 객체여야 합니다.");
        }
        JsonNode hints = mapData.get("hints");
        if (hints != null && !hints.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mapData.hints는 배열이어야 합니다.");
        }
        return objectMapper.writeValueAsString(mapData);
    }

    private void checkDuplicate(Long selfId, Long operationId, int stageNo) {
        if (stageRepository.existsByOperationIdAndStageNoAndIdNot(operationId, stageNo, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이 작전에 이미 " + stageNo + "번 스테이지가 있습니다.");
        }
    }

    private void saveAndFlush(Stage stage) {
        try {
            stageRepository.saveAndFlush(stage);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이 작전에 같은 번호의 스테이지가 이미 있습니다.");
        }
    }

    private AdminStageResponse toResponse(Stage stage) {
        JsonNode mapData = objectMapper.readTree(stage.getMapData());
        JsonNode hints = mapData.get("hints");
        return new AdminStageResponse(
                stage.getId(),
                stage.getOperation().getId(),
                stage.getStageNo(),
                stage.getStageType(),
                stage.getTitle(),
                mapData,
                hints != null && hints.isArray() ? hints.size() : 0,
                stage.getParMoves(),
                stage.getRewardCoin(),
                stage.getCreatedAt(),
                stage.getUpdatedAt());
    }
}
