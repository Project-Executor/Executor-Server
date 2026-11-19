package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapDataParserTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final MapDataParser parser = new MapDataParser(json);

    @Test
    void 클라이언트에_보낼_맵은_저장된_JSON_그대로() {
        var map = parser.parse("{\"width\":5,\"tiles\":[\"#.#\"],\"camera\":{\"width\":9}}");

        assertThat(map.get("width").asInt()).isEqualTo(5);
        assertThat(map.get("tiles").size()).isEqualTo(1);
        assertThat(map.get("camera").get("width").asInt()).isEqualTo(9);
    }

    @Test
    void 저장할_맵은_JSON_객체여야_하고_크기_제한을_넘으면_안_된다() {
        assertThat(parser.toStoredJson(json.readTree("{\"w\":3}"), 100)).isEqualTo("{\"w\":3}");
        assertThatThrownBy(() -> parser.toStoredJson(json.readTree("[1]"), 100)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> parser.toStoredJson(null, 100)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> parser.toStoredJson(json.readTree("{\"t\":\"" + "a".repeat(2048) + "\"}"), 1024))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1KB");
    }
}
