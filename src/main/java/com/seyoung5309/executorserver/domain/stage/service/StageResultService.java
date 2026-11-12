package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.repository.GuidebookRepository;
import com.seyoung5309.executorserver.domain.guidebook.repository.UserGuidebookRepository;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultRequest;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse.RunResult;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse.UnlockedGuidebook;
import com.seyoung5309.executorserver.domain.stage.dto.StarsResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProfileRepository profileRepository;
    private final GuidebookRepository guidebookRepository;
    private final UserGuidebookRepository userGuidebookRepository;
    private final Clock clock;

    /**
     * 한 판의 결과를 반영한다 (하나의 트랜잭션).
     * <ol>
     *   <li>기록 행을 만들거나 도전 횟수 +1 (INSERT ... ON DUPLICATE KEY UPDATE, 행 잠금)</li>
     *   <li>행 잠금(SELECT ... FOR UPDATE)으로 기록을 읽는다 -> 같은 기록에 대한 동시 요청은 순서대로 처리된다</li>
     *   <li>클리어했으면 최소 기록·숨겨진 요소 갱신, 처음 클리어할 때만 보상 코인 지급 (공식 스테이지만)</li>
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
        Instant now = clock.instant();

        userStageRepository.upsertPlay(userId, stageId, now);
        UserStage record = userStageRepository.findForUpdate(userId, stageId)
                .orElseThrow(() -> new IllegalStateException("기록 행이 없습니다: userId=" + userId + ", stageId=" + stageId));

        boolean isNewBest = false;
        boolean firstClear = false;
        if (cleared) {
            UserStage.ClearResult result = record.applyClear(request.moves(), request.hiddenFoundOrFalse(), now);
            isNewBest = result.isNewBest();
            firstClear = result.firstClear();
        }

        // 사용자 맵은 보상이 없다 (코인 파밍 방지). DB 값과 상관없이 서버에서 한 번 더 막는다.
        int rewardCoin = firstClear && stage.isOfficial() ? stage.getRewardCoin() : 0;
        if (rewardCoin > 0) {
            profileRepository.addCoin(userId, rewardCoin);
        }
        int totalCoin = profileRepository.findCoinByUserId(userId)
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
        boolean tutorial = stage.getStageType() == StageType.TUTORIAL;
        boolean par = clear && !tutorial && request.moves() <= stage.getParMoves();
        boolean hidden = clear && !tutorial && request.hiddenFoundOrFalse();
        return new RunResult(clear, par, hidden);
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
        List<Stage> ordered = stageRepository.findAllOfficialInPlayOrder();
        for (int i = 0; i < ordered.size() - 1; i++) {
            if (ordered.get(i).getId().equals(stage.getId())) {
                return ordered.get(i + 1).getId();
            }
        }
        return null;
    }
}
