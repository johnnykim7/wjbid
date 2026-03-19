package com.biddingagency;

import com.biddingagency.common.BaseEntity;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * 테스트용 헬퍼 - BaseEntity의 id를 리플렉션으로 설정
 */
public class TestHelper {

    public static <T extends BaseEntity> T withId(T entity, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id via reflection", e);
        }
        return entity;
    }

    public static <T extends BaseEntity> T withId(T entity) {
        return withId(entity, UUID.randomUUID());
    }
}
