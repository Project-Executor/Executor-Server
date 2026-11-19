package com.seyoung5309.executorserver.domain.wordlist.controller;

import com.seyoung5309.executorserver.domain.wordlist.dto.WordListResponse;
import com.seyoung5309.executorserver.domain.wordlist.service.WordListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "단어 목록", description = "감염된 컴퓨터 타자 해킹에 쓰는 단어. 로그인 없이 볼 수 있다 (오프라인 테스트용)")
@RestController
@RequestMapping("/api/word-lists")
@RequiredArgsConstructor
public class WordListController {

    private final WordListService wordListService;

    @Operation(summary = "단어 목록", description = "맵의 computer.wordListId로 부른다. 없는 ID는 404 (게임은 기본 단어를 쓴다).")
    @GetMapping("/{wordListId}")
    public WordListResponse getWordList(@PathVariable Long wordListId) {
        return wordListService.getWordList(wordListId);
    }
}
