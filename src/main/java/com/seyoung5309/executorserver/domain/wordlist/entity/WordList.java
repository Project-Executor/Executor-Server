package com.seyoung5309.executorserver.domain.wordlist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 타자 해킹 단어 목록. 맵의 감염된 컴퓨터(computer)가 wordListId로 고르고,
 * 게임은 이 목록에서 단어를 무작위로 뽑아 치게 한다 (개수·제한 시간은 맵에서 컴퓨터마다 정한다).
 */
@Entity
@Table(name = "word_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    private String description;

    /** 단어 (입력 순서 유지). 수정하면 통째로 바꾼다 */
    @ElementCollection
    @CollectionTable(name = "word_list_words", joinColumns = @JoinColumn(name = "word_list_id"))
    @OrderColumn(name = "position")
    @Column(name = "word", nullable = false, length = 50)
    private List<String> words = new ArrayList<>();

    public WordList(String name, String description, List<String> words) {
        update(name, description, words);
    }

    public void update(String name, String description, List<String> words) {
        this.name = name;
        this.description = description;
        this.words.clear();
        this.words.addAll(words);
    }
}
