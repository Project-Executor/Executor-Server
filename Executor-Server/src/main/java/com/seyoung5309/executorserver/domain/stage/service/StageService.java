package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.MyRecordResponse;
import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.response.UserSummary;
import com.seyoung5309.executorserver.domain.stage.dto.StageSummaryResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.OperationRepository;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageService {

    private final OperationRepository operationRepository;
    private final StageRepository stageRepository;
    private final UserStageRepository userStageRepository;
    private final ProfileRepository profileRepository;
    private final StageAccessService stageAccessService;
    private final MapDataParser mapDataParser;

    /**
     * 공식 작전·스테이지 목록과 내 진행도(해금, 클리어, 별, 최고 기록).
     * 스테이지가 없는 작전도 빈 목록으로 포함한다.
     */
    public List<OperationResponse> getOperations(Long userId) {
        List<Operation> operations = operationRepository.findAllByOrderBySortOrderAsc();
        List<Stage> stages = stageRepository.findAllOfficialInPlayOrder();
        Map<Long, UserStage> records = userStageRepository.findAllOfficialByUserId(userId).stream()
                .collect(Collectors.toMap(record -> record.getStage().getId(), Function.identity()));

        Set<Long> clearedIds = records.values().stream()
                .filter(UserStage::isCleared)
                .map(record -> record.getStage().getId())
                .collect(Collectors.toSet());
        Set<Long> unlockedIds = StageUnlockPolicy.unlockedStageIds(stages, clearedIds);

        Map<Long, List<StageSummaryResponse>> stagesByOperation = new LinkedHashMap<>();
        for (Stage stage : stages) {
            stagesByOperation
                    .computeIfAbsent(stage.getOperation().getId(), id -> new ArrayList<>())
                    .add(StageSummaryResponse.of(stage, records.get(stage.getId()), unlockedIds.contains(stage.getId())));
        }

        return operations.stream()
                .map(operation -> OperationResponse.of(
                        operation, stagesByOperation.getOrDefault(operation.getId(), List.of())))
                .toList();
    }

    /**
     * 스테이지 상세와 맵 데이터(hints 제외), 내 기록.
     *
     * @throws BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN
     */
    public StageDetailResponse getStageDetail(Long userId, Long stageId) {
        Stage stage = stageAccessService.getPlayableStage(userId, stageId);
        UserStage record = userStageRepository.findByUserIdAndStageId(userId, stageId).orElse(null);
        MapDataParser.PublicMapData publicMap = mapDataParser.toPublic(stage.getMapData());

        return new StageDetailResponse(
                stage.getId(),
                stage.getOperation() == null ? null : stage.getOperation().getId(),
                stage.getStageNo(),
                stage.getStageType(),
                stage.getTitle(),
                creatorOf(stage),
                stage.getParMoves(),
                stage.getRewardCoin(),
                publicMap.hintCount(),
                publicMap.mapData(),
                MyRecordResponse.of(stage, record)
        );
    }

    private UserSummary creatorOf(Stage stage) {
        if (stage.isOfficial()) {
            return null;
        }
        return profileRepository.findById(stage.getCreator().getId())
                .map(UserSummary::from)
                .orElse(null);
    }
}
