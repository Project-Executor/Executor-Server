package com.seyoung5309.executorserver.domain.wordlist.dto;

import com.seyoung5309.executorserver.domain.wordlist.entity.WordList;

import java.util.List;

/** 게임용 단어 목록 (게임이 이 중에서 무작위로 뽑는다) */
public record WordListResponse(Long wordListId, String name, List<String> words) {

    public static WordListResponse from(WordList list) {
        return new WordListResponse(list.getId(), list.getName(), List.copyOf(list.getWords()));
    }
}
