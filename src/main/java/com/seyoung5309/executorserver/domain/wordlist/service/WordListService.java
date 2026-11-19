package com.seyoung5309.executorserver.domain.wordlist.service;

import com.seyoung5309.executorserver.domain.wordlist.dto.WordListResponse;
import com.seyoung5309.executorserver.domain.wordlist.repository.WordListRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordListService {

    private final WordListRepository wordListRepository;

    /** @throws BusinessException NOT_FOUND */
    public WordListResponse getWordList(Long wordListId) {
        return wordListRepository.findById(wordListId)
                .map(WordListResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "단어 목록을 찾을 수 없습니다."));
    }
}
