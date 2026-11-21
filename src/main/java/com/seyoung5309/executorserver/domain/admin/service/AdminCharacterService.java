package com.seyoung5309.executorserver.domain.admin.service;

import com.seyoung5309.executorserver.domain.admin.dto.AdminCharacterRequest;
import com.seyoung5309.executorserver.domain.character.dto.CharacterResponse;
import com.seyoung5309.executorserver.domain.character.entity.GameCharacter;
import com.seyoung5309.executorserver.domain.character.repository.GameCharacterRepository;
import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 인물 관리. 맵이 쓰는지는 검사하지 않는다 (지운 인물의 대사는 게임이 기본 이름표 색을 쓴다) */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCharacterService {

    /** 새로 만들 때 중복 검사에서 제외할 ID (실제 ID는 1부터) */
    private static final Long NEW = 0L;

    private final GameCharacterRepository characterRepository;

    @Transactional(readOnly = true)
    public List<CharacterResponse> getAll() {
        return characterRepository.findAllByOrderByIdAsc().stream().map(CharacterResponse::from).toList();
    }

    /** @throws BusinessException DUPLICATE_VALUE(ID·이름 중복) */
    public CharacterResponse create(AdminCharacterRequest request) {
        Values v = Values.of(request);
        checkDuplicate(NEW, v);
        GameCharacter character = new GameCharacter(v.key(), v.name(), v.color(), v.protagonist());
        saveAndFlush(character);
        return CharacterResponse.from(character);
    }

    /** @throws BusinessException NOT_FOUND, DUPLICATE_VALUE */
    public CharacterResponse update(Long characterId, AdminCharacterRequest request) {
        GameCharacter character = find(characterId);
        Values v = Values.of(request);
        checkDuplicate(characterId, v);
        character.update(v.key(), v.name(), v.color(), v.protagonist());
        saveAndFlush(character);
        return CharacterResponse.from(character);
    }

    /** @throws BusinessException NOT_FOUND */
    public void delete(Long characterId) {
        characterRepository.delete(find(characterId));
    }

    /** 앞뒤 공백을 지우고 색은 대문자로 */
    private record Values(String key, String name, String color, boolean protagonist) {
        static Values of(AdminCharacterRequest r) {
            return new Values(r.characterKey().strip(), r.name().strip(), r.tagColor().strip().toUpperCase(Locale.ROOT),
                    Boolean.TRUE.equals(r.protagonist()));
        }
    }

    private GameCharacter find(Long characterId) {
        return characterRepository.findById(Objects.requireNonNull(characterId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "인물을 찾을 수 없습니다."));
    }

    private void checkDuplicate(Long selfId, Values v) {
        if (characterRepository.existsByCharacterKeyAndIdNot(v.key(), selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 인물 ID입니다: " + v.key());
        }
        if (characterRepository.existsByNameAndIdNot(v.name(), selfId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "이미 있는 인물 이름입니다: " + v.name());
        }
    }

    private void saveAndFlush(GameCharacter character) {
        try {
            characterRepository.saveAndFlush(character);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_VALUE, "인물 ID나 이름이 이미 쓰이고 있습니다.");
        }
    }
}
