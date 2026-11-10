package com.seyoung5309.executorserver.domain.stage.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MapDataParserTest {

    private final MapDataParser parser = new MapDataParser(JsonMapper.builder().build());

    private static final String MAP = """
            {"width":5,"height":3,"tiles":[[0,1],[1,0]],"hints":["오른쪽 벽","열쇠 먼저",{"text":"객체 힌트"}]}
            """;

    @Test
    void 클라이언트에_보낼_맵에서_hints를_빼고_개수만_센다() {
        MapDataParser.PublicMapData result = parser.toPublic(MAP);

        assertThat(result.hintCount()).isEqualTo(3);
        assertThat(result.mapData().has("hints")).isFalse();
        assertThat(result.mapData().get("width").asInt()).isEqualTo(5);
        assertThat(result.mapData().get("tiles").size()).isEqualTo(2);
    }

    @Test
    void hints가_없으면_개수는_0이고_맵은_그대로() {
        MapDataParser.PublicMapData result = parser.toPublic("{\"width\":5}");

        assertThat(result.hintCount()).isZero();
        assertThat(result.mapData().get("width").asInt()).isEqualTo(5);
    }

    @Test
    void hints가_배열이_아니면_개수는_0이고_필드는_제거된다() {
        MapDataParser.PublicMapData result = parser.toPublic("{\"hints\":\"문자열\"}");

        assertThat(result.hintCount()).isZero();
        assertThat(result.mapData().has("hints")).isFalse();
    }

    @Test
    void 최상위가_객체가_아니면_그대로_돌려준다() {
        MapDataParser.PublicMapData result = parser.toPublic("[1,2,3]");

        assertThat(result.hintCount()).isZero();
        assertThat(result.mapData().isArray()).isTrue();
    }

    @Test
    void 힌트_목록을_순서대로_꺼낸다() {
        assertThat(parser.hints(MAP))
                .extracting(JsonNode::toString)
                .containsExactly("\"오른쪽 벽\"", "\"열쇠 먼저\"", "{\"text\":\"객체 힌트\"}");
    }

    @Test
    void 힌트가_없으면_빈_목록() {
        assertThat(parser.hints("{}")).isEmpty();
    }
}
