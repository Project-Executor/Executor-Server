package com.seyoung5309.executorserver.domain.guestbook.service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 방명록 글의 욕설·개인정보를 * 로 가린다 (전시장 화면에 그대로 보이므로 저장하기 전에).
 * 완벽한 필터가 아니라 흔한 경우를 막는 정도이고, 나머지는 관리자 웹에서 지운다.
 */
public final class GuestbookFilter {

    private GuestbookFilter() {
    }

    /** 전화번호 (010-1234-5678, 01012345678, 010 1234 5678 등) */
    private static final Pattern PHONE = Pattern.compile("01[016789][-\\s.]?\\d{3,4}[-\\s.]?\\d{4}");
    /** 이메일 */
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    /** 주민등록번호 꼴 (6자리-7자리) */
    private static final Pattern RRN = Pattern.compile("\\d{6}[-\\s]?[1-4]\\d{6}");

    /** 가릴 말 (띄어쓰기·특수문자를 끼워 넣어도 잡도록 글자 사이에 [^가-힣a-z]*를 허용한다) */
    private static final List<String> WORDS = List.of(
            "시발", "씨발", "씨빨", "ㅅㅂ", "ㅆㅂ", "병신", "븅신", "ㅂㅅ", "좆", "존나", "졸라", "개새끼", "개새",
            "미친놈", "미친년", "닥쳐", "꺼져", "지랄", "ㅈㄹ", "엠창", "느금", "니애미", "니애비", "fuck", "shit", "bitch");

    private static final List<Pattern> WORD_PATTERNS = WORDS.stream()
            .map(w -> Pattern.compile(String.join("[^가-힣ㄱ-ㅎa-zA-Z]{0,2}",
                    w.codePoints().mapToObj(c -> Pattern.quote(new String(Character.toChars(c)))).toList()),
                    Pattern.CASE_INSENSITIVE))
            .toList();

    public static String mask(String text) {
        if (text == null || text.isEmpty()) return text;
        String out = text;
        out = replaceAll(out, PHONE);
        out = replaceAll(out, EMAIL);
        out = replaceAll(out, RRN);
        for (Pattern p : WORD_PATTERNS) out = replaceAll(out, p);
        return out;
    }

    private static String replaceAll(String text, Pattern pattern) {
        return pattern.matcher(text).replaceAll(m -> "*".repeat(m.group().codePointCount(0, m.group().length())));
    }
}
