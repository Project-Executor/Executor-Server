package com.seyoung5309.executorserver.domain.profile.dto;

/** @param avatarVersion 사진을 바꾼 시각(초). 게임은 이 값이 바뀌면 사진을 다시 받는다 */
public record AvatarVersionResponse(long avatarVersion) {
}
