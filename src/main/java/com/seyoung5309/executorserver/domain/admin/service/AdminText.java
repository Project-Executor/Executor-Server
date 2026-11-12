package com.seyoung5309.executorserver.domain.admin.service;

/** 관리자 입력 문자열 정리 */
final class AdminText {

    private AdminText() {
    }

    /** 앞뒤 공백을 지우고, 비어 있으면 null (선택 입력 항목용) */
    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
