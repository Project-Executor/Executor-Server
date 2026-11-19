package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 스테이지의 별 개수 (map_data의 stars.count, docs/map-data.md 3.4).
 * 튜토리얼은 항상 1개, 그 밖에는 stars.count(1~3, 없거나 잘못되면 3).
 * 별 2·3의 조건은 게임이 판정해서 결과 제출의 parAchieved·hiddenFound로 보낸다. 서버는 별 개수만 해석한다.
 */
public final class StarRules {

    public static final int MAX_STARS = 3;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private StarRules() {
    }

    /** 이 스테이지에서 받을 수 있는 별 수 (1~3) */
    public static int starCount(Stage stage) {
        if (stage.getStageType() == StageType.TUTORIAL) {
            return 1;
        }
        return starCount(stage.getMapData());
    }

    /** map_data의 stars.count. 없거나 1~3이 아니면 3 */
    static int starCount(String mapData) {
        try {
            JsonNode value = JSON.readTree(mapData).path("stars").path("count");
            if (value.isIntegralNumber() && value.asInt() >= 1 && value.asInt() <= MAX_STARS) {
                return value.asInt();
            }
        } catch (RuntimeException e) {
            // 읽을 수 없는 map_data는 기본값
        }
        return MAX_STARS;
    }
}
