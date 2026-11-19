package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminLayerRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminLayerResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Layer;
import com.seyoung5309.executorserver.domain.stage.repository.LayerRepository;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 레이어(스토리 대주제) 관리. 작전이 남아 있는 레이어는 지울 수 없다 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminLayerService {

    /** 새로 만들 때 중복 검사에서 제외할 ID (실제 ID는 1부터) */
    private static final Long NEW = 0L;

    private final LayerRepository layerRepository;
    private final OperationRepository operationRepository;

    /** 레이어 전체 (순서대로) + 소속 작전 수 */
    @Transactional(readOnly = true)
    public List<AdminLayerResponse> getAll() {
        return layerRepository.findAllByOrderBySortOrderAsc().stream()
                .map(layer -> AdminLayerResponse.of(layer, operationRepository.countByLayer_Id(layer.getId())))
                .toList();
    }

    /** @throws BusinessException DUPLICATE_VALUE(이름 또는 순서 중복) */
    public AdminLayerResponse create(AdminLayerRequest request) {
        String name = request.name().strip();
        checkDuplicate(NEW, name, request.sortOrder());
        Layer layer = new Layer(name, AdminText.blankToNull(request.description()), request.sortOrder());
        saveAndFlush(layer);
        return AdminLayerResponse.of(layer, 0);
    }

    /** @throws BusinessException NOT_FOUND, DUPLICATE_VALUE */
    public AdminLayerResponse update(Long layerId, AdminLayerRequest request) {
        Layer layer = find(layerId);
        String name = request.name().strip();
        checkDuplicate(layerId, name, request.sortOrder());
        layer.update(name, AdminText.blankToNull(request.description()), request.sortOrder());
        saveAndFlush(layer);
        return AdminLayerResponse.of(layer, operationRepository.countByLayer_Id(layerId));
    }

    /** @throws BusinessException NOT_FOUND, LAYER_HAS_OPERATIONS */
    public void delete(Long layerId) {
        Layer layer = find(layerId);
        if (operationRepository.existsByLayer_Id(layerId)) {
            throw new BusinessException(ErrorCode.LAYER_HAS_OPERATIONS);
        }
        try {
            layerRepository.delete(layer);
            layerRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 확인한 뒤 그 사이에 작전이 추가된 경우 (FK RESTRICT)
            throw new BusinessException(ErrorCode.LAYER_HAS_OPERATIONS);
        }
    }

    private Layer find(Long layerId) {
        return layerRepository.findById(layerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "레이어를 찾을 수 없습니다."));
    }

    private void checkDuplicate(Long selfId, String name, int sortOrder) {
        if (layerRepository.existsByNameAndIdNot(name, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 레이어 이름입니다.");
        }
        if (layerRepository.existsBySortOrderAndIdNot(sortOrder, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "sortOrder " + sortOrder + "은(는) 다른 레이어가 쓰고 있습니다.");
        }
    }

    private void saveAndFlush(Layer layer) {
        try {
            layerRepository.saveAndFlush(layer);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "레이어 이름 또는 sortOrder가 이미 쓰이고 있습니다.");
        }
    }
}
