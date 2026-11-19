package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.wordlist.entity.WordList;

import java.util.List;

public record AdminWordListResponse(Long wordListId, String name, String description, List<String> words) {

    public static AdminWordListResponse from(WordList list) {
        return new AdminWordListResponse(list.getId(), list.getName(), list.getDescription(), List.copyOf(list.getWords()));
    }
}
