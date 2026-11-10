package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.friend.repository.FriendRepository;
import com.seyoung5309.executorserver.domain.stage.dto.RankingResponse;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.repository.RankingRow;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageRankingService {

    private final StageAccessService stageAccessService;
    private final UserStageRepository userStageRepository;
    private final FriendRepository friendRepository;

    /**
     * 스테이지 최소 턴 순위.
     * 정렬: 최소 턴 -> 먼저 클리어한 순 -> 사용자 ID 순. 같은 순위는 없다 (1, 2, 3, ...).
     *
     * @throws com.seyoung5309.executorserver.global.exception.BusinessException NOT_FOUND, STAGE_LOCKED, FORBIDDEN
     */
    public RankingResponse getRanking(Long userId, Long stageId, RankingScope scope, int size) {
        stageAccessService.getPlayableStage(userId, stageId);
        PageRequest top = PageRequest.of(0, size);

        Set<Long> members = null;
        List<RankingRow> rows;
        if (scope == RankingScope.FRIENDS) {
            members = new HashSet<>(friendRepository.findFriendIds(userId));
            members.add(userId);
            rows = userStageRepository.findRankingAmong(stageId, members, top);
        } else {
            rows = userStageRepository.findRanking(stageId, top);
        }

        List<RankingResponse.Entry> entries = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            RankingRow row = rows.get(i);
            entries.add(new RankingResponse.Entry(
                    i + 1,
                    new UserSummary(row.userId(), row.nickname(), row.equippedSkinId()),
                    row.bestMoves()));
        }

        return new RankingResponse(entries, myRank(userId, stageId, members));
    }

    /** @param members null이면 전체 기준 */
    private RankingResponse.MyRank myRank(Long userId, Long stageId, Set<Long> members) {
        UserStage mine = userStageRepository.findByUserIdAndStageId(userId, stageId)
                .filter(UserStage::isCleared)
                .orElse(null);
        if (mine == null) {
            return null;
        }
        long ahead = members == null
                ? userStageRepository.countAhead(stageId, mine.getBestMoves(), mine.getFirstClearedAt(), userId)
                : userStageRepository.countAheadAmong(stageId, members, mine.getBestMoves(), mine.getFirstClearedAt(), userId);
        return new RankingResponse.MyRank(ahead + 1, mine.getBestMoves());
    }
}
