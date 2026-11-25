package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.UserRepository;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultRequest;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse.RunResult;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse.UnlockedGuidebook;
import com.seyoung5309.executorserver.domain.stage.dto.StarsResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StageResultService {

    private final StageAccessService stageAccessService;
    private final StageRepository stageRepository;
    private final UserStageRepository userStageRepository;
    private final UserRepository userRepository;
    private final GuidebookRepository guidebookRepository;
    private final UserGuidebookRepository userGuidebookRepository;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    /**
     * 한 판의 결과를 반영한다 (하나의 트랜잭션).
     * <ol>
     *   <li>기록 행을 만들거나 도전 횟수 +1 (INSERT ... ON DUPLICATE KEY UPDATE, 행 잠금)</li>
     *   <li>행 잠금(SELECT ... FOR UPDATE)으로 기록을 읽는다 -> 같은 기록에 대한 동시 요청은 순서대로 처리된다</li>
     *   <li>클리어했으면 최소 기록·데이터 파편 갱신, 처음 클리어할 때만 보상 코인 지급 (공식 스테이지만)</li>
     *   <li>만난 도감 항목 해금 (이미 해금된 항목은 건너뜀)</li>
     * </ol>
     * 같은 결과를 다시 보내도(네트워크 재전송) 보상은 한 번만 지급된다.
     *
     * @throws BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN, INVALID_INPUT
     */
    @Transactional
    public StageResultResponse submit(Long userId, Long stageId, StageResultRequest request) {
        boolean cleared = request.cleared();
        if (cleared && request.moves() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "클리어했으면 moves를 입력해 주세요.");
        }

        Stage stage = stageAccessService.getPlayableStage(userId, stageId);
        if (stage.getStageType().isEvent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이벤트 스테이지 기록은 이름을 적어 이벤트 순위에 올립니다 (POST /api/events/{stageId}/records).");
        }
        Instant now = clock.instant();

        userStageRepository.upsertPlay(userId, stageId, now);
        UserStage record = userStageRepository.findForUpdate(userId, stageId)
                .orElseThrow(() -> new IllegalStateException("기록 행이 없습니다: userId=" + userId + ", stageId=" + stageId));

        boolean isNewBest = false;
        boolean firstClear = false;
        if (cleared) {
            int stars = StarRules.starCount(stage);
            UserStage.ClearResult result = record.applyClear(request.moves(),
                    stars >= 3 && request.hiddenFoundOrFalse(), stars >= 2 && isStar2(stage, request), now);
            isNewBest = result.isNewBest();
            firstClear = result.firstClear();
        }

        // 사용자 맵은 보상이 없다 (코인 파밍 방지). DB 값과 상관없이 서버에서 한 번 더 막는다.
        int rewardCoin = firstClear && stage.isOfficial() ? stage.getRewardCoin() : 0;
        if (rewardCoin > 0) {
            userRepository.addCoin(userId, rewardCoin);
        }
        int totalCoin = userRepository.findCoinByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        List<UnlockedGuidebook> unlocked = unlockGuidebooks(userId, request.encounteredOrEmpty(), now);

        return new StageResultResponse(
                cleared,
                thisRun(stage, request),
                StarsResponse.of(stage, record),
                record.getBestMoves(),
                isNewBest,
                firstClear,
                rewardCoin,
                totalCoin,
                unlocked,
                cleared ? nextOfficialStageId(stage) : null
        );
    }

    private RunResult thisRun(Stage stage, StageResultRequest request) {
        boolean clear = request.cleared();
        int stars = StarRules.starCount(stage);
        boolean par = clear && stars >= 2 && isStar2(stage, request);
        boolean hidden = clear && stars >= 3 && request.hiddenFoundOrFalse();
        return new RunResult(clear, par, hidden);
    }

    /**
     * 별 2 조건 달성. 게임이 판정한 parAchieved가 있으면 그것을 쓰고(조건은 맵마다 다름),
     * 없으면(예전 클라이언트) 불안정 체크포인트 수가 맵의 허용 개수(stars.maxUnstable, 기본 0) 이하인지로 판정한다
     */
    private boolean isStar2(Stage stage, StageResultRequest request) {
        if (request.parAchieved() != null) {
            return request.parAchieved();
        }
        return request.unstableCountOrZero() <= maxUnstable(stage.getMapData());
    }

    /** map_data의 stars.maxUnstable. 없거나 잘못된 값이면 0 */
    int maxUnstable(String mapData) {
        try {
            JsonNode value = objectMapper.readTree(mapData).path("stars").path("maxUnstable");
            return value.isIntegralNumber() && value.asInt() >= 0 ? value.asInt() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** 존재하는 도감 항목 중 처음 만난 것만 해금하고 돌려준다. 없는 ID는 무시한다. */
    private List<UnlockedGuidebook> unlockGuidebooks(Long userId, List<Long> encounteredIds, Instant now) {
        if (encounteredIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(encounteredIds));
        List<UnlockedGuidebook> unlocked = new ArrayList<>();
        for (Guidebook guidebook : guidebookRepository.findAllById(distinctIds)) {
            if (userGuidebookRepository.unlock(userId, guidebook.getId(), now) == 1) {
                unlocked.add(UnlockedGuidebook.from(guidebook));
            }
        }
        return unlocked;
    }

    /** 플레이 순서상 다음 공식 스테이지. 사용자 맵이거나 마지막 스테이지면 null */
    private Long nextOfficialStageId(Stage stage) {
        if (!stage.isOfficial()) {
            return null;
        }
        List<Stage> ordered = stageRepository.findAllOfficialVisibleInPlayOrder();
        for (int i = 0; i < ordered.size() - 1; i++) {
            if (ordered.get(i).getId().equals(stage.getId())) {
                return ordered.get(i + 1).getId();
            }
        }
        return null;
    }
}
