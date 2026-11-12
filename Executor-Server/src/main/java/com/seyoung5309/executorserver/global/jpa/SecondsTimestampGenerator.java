package com.seyoung5309.executorserver.global.jpa;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/** 초 단위로 끊은 현재 시각(UTC)을 만든다. ClockConfig의 Clock.tick(1초)과 같은 정밀도 */
public abstract class SecondsTimestampGenerator implements BeforeExecutionGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
                           EventType eventType) {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /** {@link CreatedAtSeconds} */
    public static class Insert extends SecondsTimestampGenerator {
        @Override
        public EnumSet<EventType> getEventTypes() {
            return EventTypeSets.INSERT_ONLY;
        }
    }

    /** {@link UpdatedAtSeconds} */
    public static class InsertAndUpdate extends SecondsTimestampGenerator {
        @Override
        public EnumSet<EventType> getEventTypes() {
            return EventTypeSets.INSERT_AND_UPDATE;
        }
    }
}
