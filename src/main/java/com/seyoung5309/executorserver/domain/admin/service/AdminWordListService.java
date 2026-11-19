package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminWordListRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminWordListResponse;
import com.seyoung5309.executorserver.domain.wordlist.entity.WordList;
import com.seyoung5309.executorserver.domain.wordlist.repository.WordListRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** 타자 해킹 단어 목록 관리. 맵이 쓰는지는 검사하지 않는다 (지워진 목록을 쓰는 맵은 게임이 기본 단어를 쓴다) */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminWordListService {

    /** 새로 만들 때 중복 검사에서 제외할 ID (실제 ID는 1부터) */
    private static final Long NEW = 0L;
    static final int MAX_WORD_LENGTH = 50;

    private final WordListRepository wordListRepository;

    @Transactional(readOnly = true)
    public List<AdminWordListResponse> getAll() {
        return wordListRepository.findAllByOrderByIdAsc().stream().map(AdminWordListResponse::from).toList();
    }

    /** @throws BusinessException DUPLICATE_VALUE(이름 중복), INVALID_INPUT(단어 없음·너무 긴 단어) */
    public AdminWordListResponse create(AdminWordListRequest request) {
        String name = request.name().strip();
        checkDuplicate(NEW, name);
        WordList list = new WordList(name, AdminText.blankToNull(request.description()), normalize(request.words()));
        saveAndFlush(list);
        return AdminWordListResponse.from(list);
    }

    /** @throws BusinessException NOT_FOUND, DUPLICATE_VALUE, INVALID_INPUT */
    public AdminWordListResponse update(Long wordListId, AdminWordListRequest request) {
        WordList list = find(wordListId);
        String name = request.name().strip();
        checkDuplicate(wordListId, name);
        list.update(name, AdminText.blankToNull(request.description()), normalize(request.words()));
        saveAndFlush(list);
        return AdminWordListResponse.from(list);
    }

    /** @throws BusinessException NOT_FOUND */
    public void delete(Long wordListId) {
        wordListRepository.delete(find(wordListId));
    }

    /** 앞뒤 공백 제거, 빈 줄·중복 제거 (처음 나온 순서 유지) */
    static List<String> normalize(List<String> words) {
        var result = new LinkedHashSet<String>();
        for (String word : words) {
            if (word == null) continue;
            String w = word.strip();
            if (w.isEmpty()) continue;
            if (w.length() > MAX_WORD_LENGTH) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "단어는 " + MAX_WORD_LENGTH + "자 이하여야 합니다: " + w);
            }
            result.add(w);
        }
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "단어를 1개 이상 입력해 주세요.");
        }
        return List.copyOf(result);
    }

    private WordList find(Long wordListId) {
        return wordListRepository.findById(Objects.requireNonNull(wordListId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "단어 목록을 찾을 수 없습니다."));
    }

    private void checkDuplicate(Long selfId, String name) {
        if (wordListRepository.existsByNameAndIdNot(name, selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 단어 목록 이름입니다.");
        }
    }

    private void saveAndFlush(WordList list) {
        try {
            wordListRepository.saveAndFlush(list);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "단어 목록 이름이 이미 쓰이고 있습니다.");
        }
    }
}
