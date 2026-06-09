package model;

public class NearbyStopCandidate {
    private final BusStop stop;
    private final double distanceMeters;

    public NearbyStopCandidate(BusStop stop, double distanceMeters) {
        this.stop = stop;
        this.distanceMeters = distanceMeters;
    }

    public BusStop getStop() {
        return stop;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public int getRoundedDistanceMeters() {
        return (int) Math.round(distanceMeters);
    }
}
