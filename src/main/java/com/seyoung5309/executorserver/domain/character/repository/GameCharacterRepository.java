package com.seyoung5309.executorserver.domain.character.repository;

import com.seyoung5309.executorserver.domain.character.entity.GameCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {

    List<GameCharacter> findAllByOrderByIdAsc();

    boolean existsByCharacterKeyAndIdNot(String characterKey, Long id);

    boolean existsByNameAndIdNot(String name, Long id);
}
