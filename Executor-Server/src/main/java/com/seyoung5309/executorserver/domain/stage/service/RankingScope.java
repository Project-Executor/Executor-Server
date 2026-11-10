package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;

import java.util.Locale;

/** 순위 범위. all = 전체, friends = 나와 친구만 */
public enum RankingScope {
    ALL, FRIENDS;

    /** 대소문자 구분 없이 읽는다. 잘못된 값이면 INVALID_INPUT */
    public static RankingScope parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "scope는 all 또는 friends여야 합니다.");
        }
    }
}
