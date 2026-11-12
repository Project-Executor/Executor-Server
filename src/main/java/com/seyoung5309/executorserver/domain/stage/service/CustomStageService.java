package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.auth.repository.UserRepository;
import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import com.seyoung5309.executorserver.domain.profile.repository.ProfileRepository;
import com.seyoung5309.executorserver.domain.stage.dto.CustomStageRequest;
import com.seyoung5309.executorserver.domain.stage.dto.CustomStageStateResponse;
import com.seyoung5309.executorserver.domain.stage.dto.MyCustomStageResponse;
import com.seyoung5309.executorserver.domain.stage.dto.PublicCustomStageResponse;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.repository.StageClearCount;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageHintRepository;
import com.seyoung5309.executorserver.domain.stage.repository.UserStageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.response.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 사용자 제작 맵 관리. 플레이(상세 조회, 결과 제출, 순위)는 공식 스테이지와 같은 /api/stages API를 쓴다.
 * <ul>
 *     <li>저장하면 비공개. 비공개 맵은 제작자만 보고 플레이할 수 있다 (StageAccessService)</li>
 *     <li>제작자가 클리어한 맵(verified)만 공개할 수 있다</li>
 *     <li>공개된 맵은 수정할 수 없고, 비공개 맵을 수정하면 그 맵의 기록과 힌트 열람 기록을 모두 지운다 (다시 클리어해야 공개 가능)</li>
 *     <li>보상 코인은 항상 0</li>
 * </ul>
 * 수정·공개 전환·삭제는 스테이지 행을 잠그고 처리한다. 수정(기록 삭제)과 공개 전환이 동시에 와서
 * 검증되지 않은 내용이 공개되는 일을 막기 위함이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomStageService {

    /** map_data 최대 크기 (UTF-8 기준) */
    static final int MAX_MAP_DATA_BYTES = 256 * 1024;

    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final StageRepository stageRepository;
    private final UserStageRepository userStageRepository;
    private final UserStageHintRepository userStageHintRepository;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final MapDataParser mapDataParser;

    /** 공개된 사용자 맵 (최신순). 클리어한 사람 수와 내 클리어 여부를 붙인다 */
    @Transactional(readOnly = true)
    public PageResponse<PublicCustomStageResponse> getPublicMaps(Long userId, int page, int size) {
        Page<Stage> stages = stageRepository.findPublicUserMaps(PageRequest.of(page, size, LATEST_FIRST));
        List<Long> stageIds = stages.map(Stage::getId).getContent();
        if (stageIds.isEmpty()) {
            return PageResponse.of(stages, stage -> null);
        }
        Map<Long, Long> clearCounts = userStageRepository.countClears(stageIds).stream()
                .collect(Collectors.toMap(StageClearCount::stageId, StageClearCount::count));
        Set<Long> myCleared = userStageRepository.findClearedStageIdsAmong(userId, stageIds);
        Map<Long, Profile> creators = profileRepository.findAllById(
                        stages.map(stage -> stage.getCreator().getId()).getContent()).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        return PageResponse.of(stages, stage -> {
            Profile creator = creators.get(stage.getCreator().getId());
            return new PublicCustomStageResponse(
                    stage.getId(),
                    stage.getTitle(),
                    creator == null ? null : UserSummary.from(creator),
                    stage.getParMoves(),
                    clearCounts.getOrDefault(stage.getId(), 0L),
                    myCleared.contains(stage.getId()),
                    stage.getCreatedAt());
        });
    }

    /** 내가 만든 맵 전체 (비공개 포함, 최근 수정 순) */
    @Transactional(readOnly = true)
    public List<MyCustomStageResponse> getMyMaps(Long userId) {
        List<Stage> stages = stageRepository.findUserMapsByCreator(userId);
        if (stages.isEmpty()) {
            return List.of();
        }
        Set<Long> verified = userStageRepository.findClearedStageIdsAmong(userId,
                stages.stream().map(Stage::getId).toList());
        return stages.stream()
                .map(stage -> new MyCustomStageResponse(stage.getId(), stage.getTitle(), stage.getParMoves(),
                        stage.isPublic(), verified.contains(stage.getId()), stage.getUpdatedAt()))
                .toList();
    }

    /** 비공개 상태로 저장한다. @throws BusinessException INVALID_INPUT(mapData) */
    public CustomStageStateResponse create(Long userId, CustomStageRequest request) {
        String mapData = mapDataParser.toStoredJson(request.mapData(), MAX_MAP_DATA_BYTES);
        Stage stage = stageRepository.save(Stage.userMap(userRepository.getReferenceById(userId),
                request.title().strip(), mapData, request.parMoves()));
        return new CustomStageStateResponse(stage.getId(), false, false);
    }

    /**
     * 비공개 맵을 수정하고, 그 맵의 기록(제작자 기록 포함)과 힌트 열람 기록을 모두 지운다.
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN(내 맵이 아님), PUBLISHED_STAGE(공개 중), INVALID_INPUT
     */
    public CustomStageStateResponse update(Long userId, Long stageId, CustomStageRequest request) {
        String mapData = mapDataParser.toStoredJson(request.mapData(), MAX_MAP_DATA_BYTES);
        Stage stage = lockMyMap(userId, stageId);
        if (stage.isPublic()) {
            throw new BusinessException(ErrorCode.PUBLISHED_STAGE);
        }
        stage.updateUserMap(request.title().strip(), mapData, request.parMoves());
        stageRepository.flush();
        userStageRepository.deleteByStageId(stageId);
        userStageHintRepository.deleteByStageId(stageId);
        return new CustomStageStateResponse(stageId, false, false);
    }

    /**
     * 공개 여부를 바꾼다. 공개하려면 제작자가 클리어한 기록이 있어야 한다. 비공개로는 언제든 바꿀 수 있다.
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN, NOT_VERIFIED
     */
    public CustomStageStateResponse changeVisibility(Long userId, Long stageId, boolean isPublic) {
        Stage stage = lockMyMap(userId, stageId);
        boolean verified = userStageRepository.existsByUserIdAndStageIdAndFirstClearedAtIsNotNull(userId, stageId);
        if (isPublic && !verified) {
            throw new BusinessException(ErrorCode.NOT_VERIFIED);
        }
        stage.changeVisibility(isPublic);
        stageRepository.flush();
        return new CustomStageStateResponse(stageId, isPublic, verified);
    }

    /**
     * 삭제한다. 플레이 기록은 DB에서 함께 지워지고(CASCADE), 이 맵에 대한 신고는 남고 대상이 NULL이 된다(SET NULL).
     *
     * @throws BusinessException NOT_FOUND, FORBIDDEN
     */
    public void delete(Long userId, Long stageId) {
        stageRepository.delete(lockMyMap(userId, stageId));
        stageRepository.flush();
    }

    /** 내 사용자 맵을 잠금 읽기로 가져온다. 공식 스테이지나 없는 스테이지는 NOT_FOUND, 남의 맵은 FORBIDDEN */
    private Stage lockMyMap(Long userId, Long stageId) {
        Stage stage = stageRepository.findForUpdate(stageId)
                .filter(found -> !found.isOfficial())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 맵을 찾을 수 없습니다."));
        if (!stage.getCreator().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내가 만든 맵만 관리할 수 있습니다.");
        }
        return stage;
    }
}
