package com.seyoung5309.executorserver.domain.item.entity;

public enum ItemType {
    /** 캐릭터 스킨 */
    SKIN,
    /** 화면 테마 */
    THEME,
    /** 메인 터미널 커서 모양. image 값이 모양 키 (예: block, underline-slow, bar:#ff4fa3) */
    CURSOR,
    /** 델타 꾸미기 (메인 화면 델타 얼굴 위에 겹치는 도트). image 값이 꾸미기 키 (예: antenna, sunglasses) */
    DELTA
}
