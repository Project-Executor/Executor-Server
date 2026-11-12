package com.seyoung5309.executorserver.global.jpa;

import org.hibernate.annotations.ValueGenerationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * INSERT 시각을 초 단위로 넣는다 (@CreationTimestamp 대신 사용).
 * DB 시각 컬럼이 DATETIME(초 단위)이라, 저장 직후 응답한 값과 나중에 읽은 값이 같도록 소수점 이하를 버린다.
 */
@ValueGenerationType(generatedBy = SecondsTimestampGenerator.Insert.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CreatedAtSeconds {
}
