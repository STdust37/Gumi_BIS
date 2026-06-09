package model;

public class BusRouteSummary {
    private final String routeId;
    private final String routeNumber;
    private final int stopCount;

    public BusRouteSummary(String routeId, String routeNumber, int stopCount) {
        this.routeId = routeId;
        this.routeNumber = routeNumber;
        this.stopCount = stopCount;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getRouteNumber() {
        return routeNumber;
    }

    public int getStopCount() {
        return stopCount;
    }
}
