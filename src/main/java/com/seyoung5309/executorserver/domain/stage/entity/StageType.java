package com.seyoung5309.executorserver.domain.stage.entity;

/**
 * 스테이지 종류.
 * EVENT_TURNS·EVENT_TYPING은 전시용 이벤트 스테이지다. 늘 열려 있고 해금 순서에 끼지 않으며,
 * 일반 결과(user_stages·별·코인) 대신 이름을 적어 이벤트 순위(event_records)에 올린다.
 */
public enum StageType {
    TUTORIAL, NORMAL, BOSS,
    /** 턴수 이벤트: 맵을 깨고 기록(턴)이 적을수록 높은 순위 */
    EVENT_TURNS,
    /** 타자 이벤트: 제한 시간 안에 친 단어 수가 많을수록 높은 순위 (설정은 map_data.event) */
    EVENT_TYPING;

    public boolean isEvent() {
        return this == EVENT_TURNS || this == EVENT_TYPING;
    }
}
