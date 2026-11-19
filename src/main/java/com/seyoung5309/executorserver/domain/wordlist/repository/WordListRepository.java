package com.seyoung5309.executorserver.domain.wordlist.repository;

import com.seyoung5309.executorserver.domain.wordlist.entity.WordList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordListRepository extends JpaRepository<WordList, Long> {

    List<WordList> findAllByOrderByIdAsc();

    boolean existsByNameAndIdNot(String name, Long id);
}
