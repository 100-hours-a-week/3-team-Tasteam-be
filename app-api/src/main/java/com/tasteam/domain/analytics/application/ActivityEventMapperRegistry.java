package com.tasteam.domain.analytics.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEventMapper;

/**
 * 도메인 이벤트 타입별 매퍼를 조회하는 레지스트리입니다.
 * 신규 이벤트는 매퍼 구현체 추가만으로 확장할 수 있도록 OCP 경계를 제공합니다.
 */
@Component
public class ActivityEventMapperRegistry {

	private final Map<Class<?>, ActivityEventMapper<Object>> mapperBySourceType;

	public ActivityEventMapperRegistry(List<ActivityEventMapper<?>> mappers) {
		this.mapperBySourceType = Collections.unmodifiableMap(indexMappers(mappers));
	}

	/**
	 * 입력 이벤트 타입에 대응하는 매퍼를 조회합니다.
	 *
	 * @param eventType 도메인 이벤트 타입
	 * @return 매퍼가 있으면 Optional로 반환
	 */
	public Optional<ActivityEventMapper<Object>> findMapper(Class<?> eventType) {
		if (eventType == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(mapperBySourceType.get(eventType));
	}

	private Map<Class<?>, ActivityEventMapper<Object>> indexMappers(List<ActivityEventMapper<?>> mappers) {
		Map<Class<?>, ActivityEventMapper<Object>> indexed = new LinkedHashMap<>();
		for (ActivityEventMapper<?> mapper : mappers) {
			Class<?> sourceType = mapper.sourceType();
			ActivityEventMapper<Object> duplicated = indexed.putIfAbsent(sourceType, cast(mapper));
			if (duplicated != null) {
				throw new IllegalStateException(
					"동일 이벤트 타입 매퍼가 중복 등록되었습니다. eventType=" + sourceType.getName());
			}
		}
		return indexed;
	}

	@SuppressWarnings("unchecked")
	private ActivityEventMapper<Object> cast(ActivityEventMapper<?> mapper) {
		return (ActivityEventMapper<Object>)mapper;
	}
}
