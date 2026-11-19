package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * stages.map_data(JSON) 처리. 서버는 구조를 해석하지 않고 Unity가 정한 그대로 저장·전달한다
 * (구조: docs/map-data.md).
 */
@Component
@RequiredArgsConstructor
public class MapDataParser {

    private final ObjectMapper objectMapper;

    /** 클라이언트에 보낼 맵 데이터 (저장된 JSON 그대로) */
    public JsonNode parse(String mapData) {
        return objectMapper.readTree(mapData);
    }

    /**
     * 저장할 맵 데이터를 검사하고 JSON 문자열로 바꾼다.
     * JSON 객체여야 하고 UTF-8로 maxBytes 이하여야 한다. 그 밖의 구조는 Unity가 정하므로 검사하지 않는다.
     *
     * @throws BusinessException INVALID_INPUT
     */
    public String toStoredJson(JsonNode mapData, int maxBytes) {
        if (mapData == null || !mapData.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mapData는 JSON 객체여야 합니다.");
        }
        String json = objectMapper.writeValueAsString(mapData);
        if (json.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "mapData는 " + (maxBytes / 1024) + "KB 이하여야 합니다.");
        }
        return json;
    }
}
