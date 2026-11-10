package com.seyoung5309.executorserver.domain.stage.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    public record PublicMapData(JsonNode mapData, int hintCount) {
    }
}
