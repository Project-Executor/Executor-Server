package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminCharacterRequest;
import com.seyoung5309.executorserver.domain.admin.service.AdminCharacterService;
import com.seyoung5309.executorserver.domain.character.dto.CharacterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 인물", description = "ADMIN 권한 필요. 대화창 이름표 색·일러스트 폴더·주인공 여부.")
@RestController
@RequestMapping("/api/admin/characters")
@RequiredArgsConstructor
public class AdminCharacterController {

    private final AdminCharacterService adminCharacterService;

    @Operation(summary = "인물 전체", description = "ID 순.")
    @GetMapping
    public List<CharacterResponse> getAll() {
        return adminCharacterService.getAll();
    }

    @Operation(summary = "인물 등록", description = "characterKey·name이 겹치면 409 DUPLICATE_VALUE. tagColor는 #RRGGBB.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse create(@Valid @RequestBody AdminCharacterRequest request) {
        return adminCharacterService.create(request);
    }

    @Operation(summary = "인물 수정", description = "모든 값을 보낸다.")
    @PutMapping("/{characterId}")
    public CharacterResponse update(@PathVariable Long characterId, @Valid @RequestBody AdminCharacterRequest request) {
        return adminCharacterService.update(characterId, request);
    }

    @Operation(summary = "인물 삭제", description = "맵이 쓰고 있어도 지운다 (그 대사는 게임이 기본 이름표 색을 쓴다).")
    @DeleteMapping("/{characterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long characterId) {
        adminCharacterService.delete(characterId);
    }
}
