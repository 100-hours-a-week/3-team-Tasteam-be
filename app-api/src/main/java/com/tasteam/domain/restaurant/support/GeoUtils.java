package com.tasteam.domain.restaurant.support;

public class GeoUtils {

	private static final double EARTH_RADIUS_METER = 6371000;

	public static double distanceMeter(
		double lat1, double lon1,
		double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_METER * c;
	}

	/*
	// 거리 계산
		Double distanceMeter = null;
		if (lat != null && longitude != null && restaurant.getLocation() != null) {
			distanceMeter = GeoUtils.distanceMeter(
					latitude, longitude,
					restaurant.getLocation().getY(),
					restaurant.getLocation().getX()
			);
		}
	*/
}
