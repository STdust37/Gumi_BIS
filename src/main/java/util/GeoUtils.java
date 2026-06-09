package util;

public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeoUtils() {
    }

    public static Coordinate coordinate(String xValue, String yValue) {
        double first = parseCoordinate(xValue);
        double second = parseCoordinate(yValue);
        if (!Double.isFinite(first) || !Double.isFinite(second)) {
            return Coordinate.invalid();
        }

        double longitude = first;
        double latitude = second;
        if (Math.abs(first) <= 90.0 && Math.abs(second) > 90.0) {
            longitude = second;
            latitude = first;
        }

        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
            return Coordinate.invalid();
        }
        return new Coordinate(latitude, longitude);
    }

    public static double distanceMeters(Coordinate left, Coordinate right) {
        if (!left.isValid() || !right.isValid()) {
            return Double.POSITIVE_INFINITY;
        }

        double lat1 = Math.toRadians(left.latitude);
        double lat2 = Math.toRadians(right.latitude);
        double deltaLat = Math.toRadians(right.latitude - left.latitude);
        double deltaLon = Math.toRadians(right.longitude - left.longitude);

        double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", ""));
            if (Math.abs(parsed) > 1000.0) {
                parsed = parsed / 10000000.0;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    public static final class Coordinate {
        private final double latitude;
        private final double longitude;

        private Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        private static Coordinate invalid() {
            return new Coordinate(Double.NaN, Double.NaN);
        }

        public boolean isValid() {
            return Double.isFinite(latitude) && Double.isFinite(longitude);
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
