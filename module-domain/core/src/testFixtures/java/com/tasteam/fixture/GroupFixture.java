package com.tasteam.fixture;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;

public final class GroupFixture {

	public static final String DEFAULT_NAME = "테스트그룹";
	public static final String DEFAULT_ADDRESS = "서울특별시 강남구";
	public static final double DEFAULT_LATITUDE = 37.5665;
	public static final double DEFAULT_LONGITUDE = 126.9780;

	private GroupFixture() {}

	public static Group create() {
		return create(DEFAULT_NAME, DEFAULT_ADDRESS);
	}

	public static Group create(String name, String address) {
		return Group.builder()
			.name(name)
			.type(GroupType.UNOFFICIAL)
			.address(address)
			.location(createPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
			.joinType(GroupJoinType.PASSWORD)
			.status(GroupStatus.ACTIVE)
			.build();
	}

	public static Group createWithStatus(GroupStatus status) {
		return Group.builder()
			.name(DEFAULT_NAME)
			.type(GroupType.UNOFFICIAL)
			.address(DEFAULT_ADDRESS)
			.location(createPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
			.joinType(GroupJoinType.PASSWORD)
			.status(status)
			.build();
	}

	private static Point createPoint(double latitude, double longitude) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}
}
