package com.seyoung5309.executorserver.domain.event.service;

import com.seyoung5309.executorserver.domain.event.dto.EventRankingResponse;
import com.seyoung5309.executorserver.domain.event.dto.EventRecordRequest;
import com.seyoung5309.executorserver.domain.event.dto.EventRecordResponse;
import com.seyoung5309.executorserver.domain.event.entity.EventRecord;
import com.seyoung5309.executorserver.domain.event.repository.EventRecordRepository;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.repository.StageRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 이벤트 스테이지 순위 (전시용, 로그인 없음).
 * 턴수 이벤트는 score(턴)가 적을수록, 타자 이벤트는 score(친 단어 수)가 많을수록 위다.
 * 같은 점수는 같은 순위이고, 먼저 올린 기록이 위에 보인다.
 */
@Service
@RequiredArgsConstructor
public class EventRecordService {

    public static final int NAME_MAX = 12;
    /** 타자 이벤트 단어 수 상한 (말이 안 되는 값을 막는다) */
    public static final int TYPING_MAX = 1000;

    private final EventRecordRepository eventRecordRepository;
    private final StageRepository stageRepository;
    private final Clock clock;

    /** @throws BusinessException NOT_FOUND(이벤트 스테이지가 아님) */
    @Transactional(readOnly = true)
    public EventRankingResponse getRanking(Long stageId, int size) {
        Stage stage = findEventStage(stageId);
        PageRequest page = PageRequest.of(0, size);
        List<EventRecord> top = ascending(stage)
                ? eventRecordRepository.findTopAscending(stageId, page)
                : eventRecordRepository.findTopDescending(stageId, page);
        List<EventRankingResponse.Entry> entries = new ArrayList<>(top.size());
        int rank = 0;
        Integer previous = null;
        for (int i = 0; i < top.size(); i++) {
            EventRecord r = top.get(i);
            if (previous == null || r.getScore() != previous) rank = i + 1;
            previous = r.getScore();
            entries.add(new EventRankingResponse.Entry(r.getId(), rank, r.getName(), r.getScore(), r.getCreatedAt()));
        }
        return new EventRankingResponse(stage.getId(), stage.getTitle(), stage.getStageType(),
                eventRecordRepository.countByStageId(stageId), entries);
    }

    /**
     * 기록을 올린다.
     *
     * @throws BusinessException NOT_FOUND(이벤트 스테이지가 아님), INVALID_INPUT(이름·점수), DUPLICATE_VALUE(같은 이름)
     */
    @Transactional
    public EventRecordResponse submit(Long stageId, EventRecordRequest request) {
        Stage stage = findEventStage(stageId);
        String name = normalizeName(request.name());
        int score = request.score();
        if (stage.getStageType() == StageType.EVENT_TURNS && score < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "턴수 이벤트의 기록은 1턴 이상이어야 합니다.");
        }
        if (stage.getStageType() == StageType.EVENT_TYPING && score > TYPING_MAX) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "타자 기록이 너무 큽니다.");
        }
        if (eventRecordRepository.existsByStageIdAndName(stageId, name)) {
            throw duplicateName();
        }
        EventRecord record = new EventRecord(stageId, name, score, clock.instant());
        try {
            eventRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw duplicateName(); // 같은 이름이 동시에 들어온 경우 (UNIQUE)
        }
        long better = ascending(stage)
                ? eventRecordRepository.countBetterAscending(stageId, score)
                : eventRecordRepository.countBetterDescending(stageId, score);
        return new EventRecordResponse(record.getId(), name, score, (int) better + 1, eventRecordRepository.countByStageId(stageId));
    }

    /** 관리자: 기록 하나 지우기 (부적절한 이름 등). @throws BusinessException NOT_FOUND */
    @Transactional
    public void delete(Long stageId, Long recordId) {
        EventRecord record = eventRecordRepository.findById(recordId)
                .filter(r -> r.getStageId().equals(stageId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기록을 찾을 수 없습니다."));
        eventRecordRepository.delete(record);
    }

    /** 관리자: 이 스테이지 기록 전부 지우기 (전시 시작 전 초기화). @return 지운 수 */
    @Transactional
    public int deleteAll(Long stageId) {
        findEventStage(stageId);
        return eventRecordRepository.deleteAllByStage(stageId);
    }

    /** 앞뒤 공백을 지우고 1~12자. 줄바꿈 같은 제어 문자는 안 된다 */
    static String normalizeName(String raw) {
        String name = raw == null ? "" : raw.strip();
        int length = name.codePointCount(0, name.length());
        if (length < 1 || length > NAME_MAX) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이름은 1~" + NAME_MAX + "자여야 합니다.");
        }
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이름에 쓸 수 없는 글자가 있습니다.");
        }
        return name;
    }

    private Stage findEventStage(Long stageId) {
        return stageRepository.findById(stageId)
                .filter(s -> s.isOfficial() && s.getStageType().isEvent())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이벤트 스테이지를 찾을 수 없습니다."));
    }

    private static boolean ascending(Stage stage) {
        return stage.getStageType() == StageType.EVENT_TURNS;
    }

    private static BusinessException duplicateName() {
        return new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 사용 중인 이름입니다.");
    }
}
