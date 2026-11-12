package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
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
public class AdminOperationService {

    /** 새로 만들 때 중복 검사에서 제외할 ID (실제 ID는 1부터) */
    private static final Long NEW = 0L;

    private final OperationRepository operationRepository;
    private final StageRepository stageRepository;

    /** @throws BusinessException DUPLICATE_VALUE(이름 또는 순서 중복) */
    public AdminOperationResponse create(AdminOperationRequest request) {
        String name = request.name().strip();
        checkDuplicate(NEW, name, request.sortOrder());

        Operation operation = new Operation(name, AdminText.blankToNull(request.description()), request.sortOrder());
        saveAndFlush(operation);
        return AdminOperationResponse.from(operation);
    }

    /** @throws BusinessException NOT_FOUND, DUPLICATE_VALUE */
    public AdminOperationResponse update(Long operationId, AdminOperationRequest request) {
        Operation operation = find(operationId);
        String name = request.name().strip();
        checkDuplicate(operationId, name, request.sortOrder());

        operation.update(name, AdminText.blankToNull(request.description()), request.sortOrder());
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

    private void checkDuplicate(Long selfId, String name, int sortOrder) {
        if (operationRepository.existsByNameAndIdNot(name, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 작전 이름입니다.");
        }
        if (operationRepository.existsBySortOrderAndIdNot(sortOrder, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "sortOrder " + sortOrder + "은(는) 다른 작전이 쓰고 있습니다.");
        }
    }

    private void saveAndFlush(Operation operation) {
        try {
            operationRepository.saveAndFlush(operation);
        } catch (DataIntegrityViolationException e) {
            // 검사한 뒤 다른 요청이 같은 값을 먼저 저장한 경우 (UNIQUE 제약)
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "작전 이름 또는 sortOrder가 이미 쓰이고 있습니다.");
        }
    }
}
