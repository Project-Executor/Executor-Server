package com.seyoung5309.executorserver.domain.stage;

import com.seyoung5309.executorserver.domain.auth.entity.User;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/** 스테이지 관련 단위 테스트용 객체 생성 도우미 (엔티티에 공개 생성자가 없어서 리플렉션으로 만든다) */
public final class StageFixtures {

    private StageFixtures() {
    }

    public static Operation operation(Long id, String name, int sortOrder) {
        Operation operation = BeanUtils.instantiateClass(Operation.class);
        ReflectionTestUtils.setField(operation, "id", id);
        ReflectionTestUtils.setField(operation, "name", name);
        ReflectionTestUtils.setField(operation, "sortOrder", sortOrder);
        return operation;
    }

    public static Stage officialStage(Long id, Operation operation, int stageNo, StageType type, int parMoves) {
        Stage stage = BeanUtils.instantiateClass(Stage.class);
        ReflectionTestUtils.setField(stage, "id", id);
        ReflectionTestUtils.setField(stage, "operation", operation);
        ReflectionTestUtils.setField(stage, "stageNo", stageNo);
        ReflectionTestUtils.setField(stage, "stageType", type);
        ReflectionTestUtils.setField(stage, "title", "stage-" + id);
        ReflectionTestUtils.setField(stage, "mapData", "{}");
        ReflectionTestUtils.setField(stage, "parMoves", parMoves);
        ReflectionTestUtils.setField(stage, "rewardCoin", 50);
        ReflectionTestUtils.setField(stage, "isPublic", true);
        return stage;
    }

    public static Stage officialStage(Long id, Operation operation, int stageNo) {
        return officialStage(id, operation, stageNo, StageType.NORMAL, 10);
    }

    /** 클리어한 기록 */
    public static UserStage cleared(Stage stage, int bestMoves, boolean hiddenFound) {
        UserStage record = record(stage, 1);
        ReflectionTestUtils.setField(record, "bestMoves", bestMoves);
        ReflectionTestUtils.setField(record, "isHiddenFound", hiddenFound);
        ReflectionTestUtils.setField(record, "firstClearedAt", Instant.parse("2026-11-10T00:00:00Z"));
        return record;
    }

    /** 플레이만 하고 클리어하지 못한 기록 */
    public static UserStage notCleared(Stage stage, int playCount) {
        return record(stage, playCount);
    }

    public static User user(Long id) {
        User user = new User("user" + id + "@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserStage record(Stage stage, int playCount) {
        UserStage record = BeanUtils.instantiateClass(UserStage.class);
        ReflectionTestUtils.setField(record, "stage", stage);
        ReflectionTestUtils.setField(record, "playCount", playCount);
        ReflectionTestUtils.setField(record, "lastPlayedAt", Instant.parse("2026-11-10T00:00:00Z"));
        return record;
    }
}
