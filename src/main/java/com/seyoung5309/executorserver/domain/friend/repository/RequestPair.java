package com.seyoung5309.executorserver.domain.friend.repository;

/** 친구 요청의 보낸 사람·받는 사람 ID (엔티티를 영속성 컨텍스트에 올리지 않고 읽기 위함) */
public record RequestPair(Long senderId, Long receiverId) {
}
