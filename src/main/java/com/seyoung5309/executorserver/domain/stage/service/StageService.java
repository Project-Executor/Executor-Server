package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.domain.stage.dto.MyRecordResponse;
import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageService {

    private final OperationRepository operationRepository;
    private final StageRepository stageRepository;
    private final UserStageRepository userStageRepository;
    private final UserRepository userRepository;
    private final StageAccessService stageAccessService;
    private final MapDataParser mapDataParser;

    /**
     * 공식 작전·스테이지 목록과 내 진행도(해금, 클리어, 별, 최고 기록). 플레이 순서(레이어 -> 작전)대로이고,
     * 비공개 레이어(sortOrder 음수)는 뺀다.
     * 작전마다 소속 레이어를 담는다. 스테이지가 없는 작전도 빈 목록으로 포함한다.
     */
    public List<OperationResponse> getOperations(Long userId) {
        List<Operation> operations = operationRepository.findAllVisibleInPlayOrder();
        List<Stage> stages = stageRepository.findAllOfficialVisibleInPlayOrder();
        Map<Long, UserStage> records = userStageRepository.findAllOfficialByUserId(userId).stream()
                .collect(Collectors.toMap(record -> record.getStage().getId(), Function.identity()));

        Set<Long> clearedIds = records.values().stream()
                .filter(UserStage::isCleared)
                .map(record -> record.getStage().getId())
                .collect(Collectors.toSet());
        Set<Long> unlockedIds = StageUnlockPolicy.unlockedStageIds(stages, clearedIds);
        return group(operations, stages, records, unlockedIds::contains);
    }

    /**
     * 게스트(로그인하지 않은 전시용 플레이)의 작전·스테이지 목록. 기록은 없고 모든 스테이지를 열린 것으로 보낸다.
     * 해금은 게임이 PC에 저장한 게스트 기록으로 정한다.
     */
    public List<OperationResponse> getGuestOperations() {
        return group(operationRepository.findAllVisibleInPlayOrder(), stageRepository.findAllOfficialVisibleInPlayOrder(),
                Map.of(), stageId -> true);
    }

    /** 작전별로 스테이지 요약을 묶는다 (스테이지가 없는 작전도 빈 목록으로) */
    private List<OperationResponse> group(List<Operation> operations, List<Stage> stages, Map<Long, UserStage> records,
                                          Predicate<Long> unlocked) {
        Map<Long, List<StageSummaryResponse>> stagesByOperation = new LinkedHashMap<>();
        for (Stage stage : stages) {
            stagesByOperation
                    .computeIfAbsent(stage.getOperation().getId(), id -> new ArrayList<>())
                    .add(StageSummaryResponse.of(stage, records.get(stage.getId()), unlocked.test(stage.getId())));
        }

        return operations.stream()
                .map(operation -> OperationResponse.of(
                        operation, stagesByOperation.getOrDefault(operation.getId(), List.of())))
                .toList();
    }

    /**
     * 스테이지 상세와 맵 데이터, 내 기록.
     *
     * @throws BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN
     */
    public StageDetailResponse getStageDetail(Long userId, Long stageId) {
        Stage stage = stageAccessService.getPlayableStage(userId, stageId);
        UserStage record = userStageRepository.findByUserIdAndStageId(userId, stageId).orElse(null);

        return new StageDetailResponse(
                stage.getId(),
                stage.getOperation() == null ? null : stage.getOperation().getId(),
                stage.getStageNo(),
                stage.getStageType(),
                stage.getTitle(),
                creatorOf(stage),
                stage.getParMoves(),
                stage.getRewardCoin(),
                mapDataParser.parse(stage.getMapData()),
                MyRecordResponse.of(stage, record)
        );
    }

    /**
     * 게스트의 스테이지 상세: 공식 스테이지만, 해금 검사 없이. 사용자 맵은 로그인해야 볼 수 있다.
     *
     * @throws BusinessException NOT_FOUND (없거나 사용자 맵)
     */
    public StageDetailResponse getGuestStageDetail(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
                .filter(s -> s.isOfficial() && !s.isHidden())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "스테이지를 찾을 수 없습니다."));
        return new StageDetailResponse(
                stage.getId(),
                stage.getOperation().getId(),
                stage.getStageNo(),
                stage.getStageType(),
                stage.getTitle(),
                null,
                stage.getParMoves(),
                stage.getRewardCoin(),
                mapDataParser.parse(stage.getMapData()),
                MyRecordResponse.of(stage, null)
        );
    }

    private UserSummary creatorOf(Stage stage) {
        if (stage.isOfficial()) {
            return null;
        }
        return userRepository.findById(stage.getCreator().getId())
                .map(UserSummary::from)
                .orElse(null);
    }
}
