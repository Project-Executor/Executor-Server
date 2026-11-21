package com.seyoung5309.executorserver.domain.character.dto;

import com.seyoung5309.executorserver.domain.character.entity.GameCharacter;

/** 인물 (게임·관리자 공용) */
public record CharacterResponse(Long characterId, String characterKey, String name, String tagColor, boolean protagonist) {

    public static CharacterResponse from(GameCharacter c) {
        return new CharacterResponse(c.getId(), c.getCharacterKey(), c.getName(), c.getTagColor(), c.isProtagonist());
    }
}
