package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import com.seyoung5309.executorserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * stages.map_data(JSON) 처리.
 * 서버는 최상위 "hints" 배열만 해석하고 나머지 구조는 Unity가 정한 그대로 전달한다.
 * <pre>
 * { "hints": ["열쇠는 오른쪽 벽 너머에 있다.", ...], ...나머지는 Unity가 정한 구조 }
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class MapDataParser {

    private static final String HINTS = "hints";

    private final ObjectMapper objectMapper;

    /** 클라이언트에 보낼 맵 데이터: hints를 뺀 JSON과 힌트 개수 */
    public PublicMapData toPublic(String mapData) {
        JsonNode root = objectMapper.readTree(mapData);
        if (!(root instanceof ObjectNode object)) {
            return new PublicMapData(root, 0);
        }
        ObjectNode copy = object.deepCopy();
        JsonNode hints = copy.remove(HINTS);
        int hintCount = hints != null && hints.isArray() ? hints.size() : 0;
        return new PublicMapData(copy, hintCount);
    }

    /** 맵 데이터의 힌트 목록. hints가 없거나 배열이 아니면 빈 목록 */
    public List<JsonNode> hints(String mapData) {
        JsonNode hints = objectMapper.readTree(mapData).get(HINTS);
        List<JsonNode> result = new ArrayList<>();
        if (hints != null && hints.isArray()) {
            hints.forEach(result::add);
        }
        return result;
    }

    /**
     * 저장할 맵 데이터를 검사하고 JSON 문자열로 바꾼다.
     * JSON 객체여야 하고, hints가 있으면 배열이어야 하며, UTF-8로 maxBytes 이하여야 한다.
     * 그 밖의 구조는 Unity가 정하므로 검사하지 않는다.
     *
     * @throws BusinessException INVALID_INPUT
     */
    public String toStoredJson(JsonNode mapData, int maxBytes) {
        if (mapData == null || !mapData.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mapData는 JSON 객체여야 합니다.");
        }
        JsonNode hints = mapData.get(HINTS);
        if (hints != null && !hints.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "mapData.hints는 배열이어야 합니다.");
        }
        String json = objectMapper.writeValueAsString(mapData);
        if (json.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "mapData는 " + (maxBytes / 1024) + "KB 이하여야 합니다.");
        }
        return json;
    }

    public record PublicMapData(JsonNode mapData, int hintCount) {
    }
}
