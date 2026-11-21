package com.seyoung5309.executorserver.domain.character.service;

import com.seyoung5309.executorserver.domain.character.dto.CharacterResponse;
import com.seyoung5309.executorserver.domain.character.repository.GameCharacterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final GameCharacterRepository characterRepository;

    public List<CharacterResponse> getAll() {
        return characterRepository.findAllByOrderByIdAsc().stream().map(CharacterResponse::from).toList();
    }
}
