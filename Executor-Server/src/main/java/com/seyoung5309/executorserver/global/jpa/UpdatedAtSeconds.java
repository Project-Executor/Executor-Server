package com.seyoung5309.executorserver.global.jpa;

import org.hibernate.annotations.ValueGenerationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** INSERT·UPDATE 시각을 초 단위로 넣는다 (@UpdateTimestamp 대신 사용). 이유는 {@link CreatedAtSeconds} 참고 */
@ValueGenerationType(generatedBy = SecondsTimestampGenerator.InsertAndUpdate.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface UpdatedAtSeconds {
}
