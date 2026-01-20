package com.tasteam.domain.common.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.EntityPathBase;

/**
 * QueryDSL 동적 정렬 유틸리티
 * Pageable의 Sort 정보를 QueryDSL OrderSpecifier로 변환
 */
public final class QueryDslOrderUtil {

	private QueryDslOrderUtil() {}

	/**
	 * Pageable의 정렬 정보를 QueryDSL OrderSpecifier 배열로 변환
	 * 화이트리스트 기반으로 허용된 필드만 정렬 가능
	 */
	public static OrderSpecifier<?>[] getOrderSpecifiers(
			Pageable pageable,
			EntityPathBase<?> qClass,
			Set<String> allowedFields) {
		List<OrderSpecifier<?>> orders = new ArrayList<>();

		if (pageable.getSort().isEmpty()) {
			return orders.toArray(new OrderSpecifier[0]);
		}

		for (Sort.Order sortOrder : pageable.getSort()) {
			String property = sortOrder.getProperty();

			if (!allowedFields.contains(property)) {
				continue;
			}

			try {
				Path<?> path = getPath(qClass, property);
				if (path instanceof ComparableExpressionBase) {
					Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
					orders.add(new OrderSpecifier<>(direction, (ComparableExpressionBase<?>)path));
				}
			} catch (Exception e) {
				continue;
			}
		}

		return orders.toArray(new OrderSpecifier[0]);
	}

	/**
	 * Map 기반 필드명 매핑을 지원하는 정렬 변환
	 * API 필드명과 엔티티 필드명이 다를 때 사용
	 */
	public static OrderSpecifier<?>[] getOrderSpecifiers(
			Pageable pageable,
			EntityPathBase<?> qClass,
			Map<String, String> fieldMapping) {
		List<OrderSpecifier<?>> orders = new ArrayList<>();

		if (pageable.getSort().isEmpty()) {
			return orders.toArray(new OrderSpecifier[0]);
		}

		for (Sort.Order sortOrder : pageable.getSort()) {
			String apiFieldName = sortOrder.getProperty();
			String entityFieldName = fieldMapping.get(apiFieldName);

			if (entityFieldName == null) {
				continue;
			}

			try {
				Path<?> path = getPath(qClass, entityFieldName);
				if (path instanceof ComparableExpressionBase) {
					Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;
					orders.add(new OrderSpecifier<>(direction, (ComparableExpressionBase<?>)path));
				}
			} catch (Exception e) {
				continue;
			}
		}

		return orders.toArray(new OrderSpecifier[0]);
	}

	/**
	 * 리플렉션을 사용하여 Q-class의 필드 Path를 가져옴
	 */
	private static Path<?> getPath(EntityPathBase<?> qClass, String fieldName) {
		try {
			return (Path<?>)qClass.getClass().getField(fieldName).get(qClass);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalArgumentException("Field not found: " + fieldName, e);
		}
	}

	/**
	 * 기본 정렬 조건을 추가하는 헬퍼 메서드
	 * 사용자 정렬이 없을 때 기본 정렬 적용
	 */
	public static OrderSpecifier<?>[] getOrderSpecifiersWithDefault(
			Pageable pageable,
			EntityPathBase<?> qClass,
			Set<String> allowedFields,
			OrderSpecifier<?> defaultOrder) {
		OrderSpecifier<?>[] orders = getOrderSpecifiers(pageable, qClass, allowedFields);

		if (orders.length == 0) {
			return new OrderSpecifier<?>[] {defaultOrder};
		}

		return orders;
	}
}
