package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminWordListRequest;
import com.seyoung5309.executorserver.domain.wordlist.entity.WordList;
import com.seyoung5309.executorserver.domain.wordlist.repository.WordListRepository;
import com.seyoung5309.executorserver.domain.wordlist.service.WordListService;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminWordListServiceTest {

    private WordListRepository repository;
    private AdminWordListService service;

    @BeforeEach
    void setUp() {
        repository = mock(WordListRepository.class);
        service = new AdminWordListService(repository);
    }

    private static WordList list(Long id, String name, String... words) {
        WordList list = new WordList(name, null, List.of(words));
        ReflectionTestUtils.setField(list, "id", id);
        return list;
    }

    private static void assertErrorCode(Runnable call, ErrorCode code) {
        assertThatThrownBy(call::run).isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    void 등록은_이름과_단어의_공백을_지우고_빈_줄과_중복을_뺀다() {
        var res = service.create(new AdminWordListRequest("  기본 명령어 ", " ",
                Arrays.asList(" sudo ", "", "git push", "sudo", null, "  ")));

        assertThat(res.name()).isEqualTo("기본 명령어");
        assertThat(res.description()).isNull();
        assertThat(res.words()).containsExactly("sudo", "git push");
        verify(repository).saveAndFlush(any());
    }

    @Test
    void 단어가_하나도_없거나_너무_길면_INVALID_INPUT() {
        assertErrorCode(() -> service.create(new AdminWordListRequest("목록", null, List.of(" ", ""))), ErrorCode.INVALID_INPUT);
        assertErrorCode(() -> service.create(new AdminWordListRequest("목록", null, List.of("a".repeat(51)))), ErrorCode.INVALID_INPUT);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void 이름이_겹치면_DUPLICATE_VALUE() {
        when(repository.existsByNameAndIdNot("기본", 0L)).thenReturn(true);
        assertErrorCode(() -> service.create(new AdminWordListRequest("기본", null, List.of("ls"))), ErrorCode.DUPLICATE_VALUE);
    }

    @Test
    void 수정하면_단어가_통째로_바뀐다() {
        WordList saved = list(1L, "기본", "ls", "cd");
        when(repository.findById(1L)).thenReturn(Optional.of(saved));

        var res = service.update(1L, new AdminWordListRequest("기본", "쉬운 명령어", List.of("pwd")));

        assertThat(res.words()).containsExactly("pwd");
        assertThat(res.description()).isEqualTo("쉬운 명령어");
        assertThat(saved.getWords()).containsExactly("pwd");
    }

    @Test
    void 없는_목록_수정_삭제는_NOT_FOUND() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertErrorCode(() -> service.update(9L, new AdminWordListRequest("x", null, List.of("ls"))), ErrorCode.NOT_FOUND);
        assertErrorCode(() -> service.delete(9L), ErrorCode.NOT_FOUND);
    }

    @Test
    void 게임용_조회는_단어를_순서대로_없으면_NOT_FOUND() {
        when(repository.findById(1L)).thenReturn(Optional.of(list(1L, "기본", "ls", "cd")));
        var game = new WordListService(repository);

        assertThat(game.getWordList(1L).words()).containsExactly("ls", "cd");
        assertErrorCode(() -> game.getWordList(2L), ErrorCode.NOT_FOUND);
    }
}
