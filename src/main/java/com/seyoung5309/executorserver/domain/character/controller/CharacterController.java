package com.seyoung5309.executorserver.domain.character.controller;

import com.seyoung5309.executorserver.domain.character.dto.CharacterResponse;
import com.seyoung5309.executorserver.domain.character.service.CharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "인물", description = "대화창 이름표 색·일러스트 폴더. 로그인 없이 볼 수 있다 (오프라인 테스트용)")
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @Operation(summary = "인물 전체", description = "ID 순. 게임은 대사의 speaker를 name으로, portrait의 인물 ID를 characterKey로 찾는다.")
    @GetMapping
    public List<CharacterResponse> getAll() {
        return characterService.getAll();
    }
}
