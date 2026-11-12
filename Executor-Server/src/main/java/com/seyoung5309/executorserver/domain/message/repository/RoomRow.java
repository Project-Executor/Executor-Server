package com.seyoung5309.executorserver.domain.message.repository;

/** 대화방 한 개 (native 쿼리 결과). 컬럼 별칭과 getter 이름이 맞아야 한다. */
public interface RoomRow {

    /** 대화 상대 ID */
    Long getPartnerId();

    /** 마지막 메시지 ID (AUTO_INCREMENT라 가장 큰 ID가 가장 최근 메시지) */
    Long getLastMessageId();

    /** 상대가 나에게 보낸 메시지 중 안 읽은 수 */
    Long getUnreadCount();
}
