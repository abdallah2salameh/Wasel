package com.wasel.backend.mobility;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    public static boolean insideBounds(double lat, double lon, double minLat, double maxLat, double minLon, double maxLon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }
}
