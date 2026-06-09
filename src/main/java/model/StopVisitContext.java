package model;

public class StopVisitContext {
    private final BusStop departureStop;
    private final BusStop destinationStop;

    public StopVisitContext(BusStop departureStop, BusStop destinationStop) {
        this.departureStop = departureStop;
        this.destinationStop = destinationStop;
    }

    public BusStop getDepartureStop() {
        return departureStop;
    }

    public BusStop getDestinationStop() {
        return destinationStop;
    }
}
