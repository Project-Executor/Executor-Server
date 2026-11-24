package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationTreeResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminStageBoardRequest;
import com.seyoung5309.executorserver.domain.stage.entity.Layer;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.LayerRepository;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminOperationService {

    /** 새로 만들 때 중복 검사에서 제외할 ID (실제 ID는 1부터) */
    private static final Long NEW = 0L;

    private final OperationRepository operationRepository;
    private final StageRepository stageRepository;
    private final LayerRepository layerRepository;

    /** 작전 전체 (플레이 순서: 레이어 -> 작전) + 소속 공식 스테이지 요약 (번호 순). 쿼리 2개 */
    @Transactional(readOnly = true)
    public List<AdminOperationTreeResponse> getAll() {
        Map<Long, List<Stage>> stagesByOperation = stageRepository.findAllOfficialInPlayOrder().stream()
                .collect(Collectors.groupingBy(s -> s.getOperation().getId()));
        return operationRepository.findAllInPlayOrder().stream()
                .map(o -> AdminOperationTreeResponse.of(o, stagesByOperation.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    /**
     * 작전 안 스테이지들의 스테이지 선택 기판 위치를 바꾼다. 목록에 없는 스테이지는 그대로 둔다.
     *
     * @throws BusinessException NOT_FOUND(작전), INVALID_INPUT(다른 작전의 스테이지, 같은 스테이지 두 번, x·y 중 하나만 있음)
     */
    public AdminOperationTreeResponse placeStages(Long operationId, AdminStageBoardRequest request) {
        Operation operation = find(operationId);
        List<Stage> stages = stageRepository.findByOperationIdOrderByStageNoAsc(operationId);
        Map<Long, Stage> byId = stages.stream().collect(Collectors.toMap(Stage::getId, Function.identity()));

        Set<Long> seen = new HashSet<>();
        for (AdminStageBoardRequest.Position p : request.positions()) {
            Stage stage = byId.get(p.stageId());
            if (stage == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "스테이지 " + p.stageId() + "는 이 작전의 스테이지가 아닙니다.");
            }
            if (!seen.add(p.stageId())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "스테이지 " + p.stageId() + "가 두 번 들어 있습니다.");
            }
            if ((p.x() == null) != (p.y() == null)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "x와 y는 둘 다 보내거나 둘 다 비워야 합니다.");
            }
            stage.placeOnBoard(p.x(), p.y());
        }
        stageRepository.flush();
        return AdminOperationTreeResponse.of(operation, stages);
    }

    /** @throws BusinessException NOT_FOUND(레이어), DUPLICATE_VALUE(이름 또는 레이어 안 순서 중복) */
    public AdminOperationResponse create(AdminOperationRequest request) {
        String name = request.name().strip();
        Layer layer = findLayer(request.layerId());
        checkDuplicate(NEW, name, layer.getId(), request.sortOrder());

        Operation operation = new Operation(layer, name, AdminText.blankToNull(request.description()), request.sortOrder());
        saveAndFlush(operation);
        return AdminOperationResponse.from(operation);
    }

    /** 레이어를 옮길 수도 있다. @throws BusinessException NOT_FOUND(작전·레이어), DUPLICATE_VALUE */
    public AdminOperationResponse update(Long operationId, AdminOperationRequest request) {
        Operation operation = find(operationId);
        String name = request.name().strip();
        Layer layer = findLayer(request.layerId());
        checkDuplicate(operationId, name, layer.getId(), request.sortOrder());

        operation.update(layer, name, AdminText.blankToNull(request.description()), request.sortOrder());
        saveAndFlush(operation);
        return AdminOperationResponse.from(operation);
    }

    /** @throws BusinessException NOT_FOUND, OPERATION_HAS_STAGES */
    public void delete(Long operationId) {
        Operation operation = find(operationId);
        if (stageRepository.existsByOperationId(operationId)) {
            throw new BusinessException(ErrorCode.OPERATION_HAS_STAGES);
        }
        try {
            operationRepository.delete(operation);
            operationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 확인한 뒤 그 사이에 스테이지가 추가된 경우 (FK RESTRICT)
            throw new BusinessException(ErrorCode.OPERATION_HAS_STAGES);
        }
    }

    private Operation find(Long operationId) {
        return operationRepository.findById(operationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "작전을 찾을 수 없습니다."));
    }

    private Layer findLayer(Long layerId) {
        return layerRepository.findById(layerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "레이어를 찾을 수 없습니다."));
    }

    private void checkDuplicate(Long selfId, String name, Long layerId, int sortOrder) {
        if (operationRepository.existsByNameAndIdNot(name, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 작전 이름입니다.");
        }
        if (operationRepository.existsByLayer_IdAndSortOrderAndIdNot(layerId, sortOrder, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE,
                    "sortOrder " + sortOrder + "은(는) 이 레이어의 다른 작전이 쓰고 있습니다.");
        }
    }

    private void saveAndFlush(Operation operation) {
        try {
            operationRepository.saveAndFlush(operation);
        } catch (DataIntegrityViolationException e) {
            // 검사한 뒤 다른 요청이 같은 값을 먼저 저장한 경우 (UNIQUE 제약)
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "작전 이름 또는 레이어 안 sortOrder가 이미 쓰이고 있습니다.");
        }
    }
}
