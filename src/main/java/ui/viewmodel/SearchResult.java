package ui.viewmodel;

import model.BusStop;
import model.Route;

public class SearchResult {
    private final BusStop stop;
    private final Route route;

    private SearchResult(BusStop stop, Route route) {
        this.stop = stop;
        this.route = route;
    }

    public static SearchResult forStop(BusStop stop) {
        return new SearchResult(stop, null);
    }

    public static SearchResult forRoute(Route route) {
        return new SearchResult(null, route);
    }

    public boolean isStop() {
        return stop != null;
    }

    public BusStop getStop() {
        return stop;
    }

    public Route getRoute() {
        return route;
    }

    public String getTitle() {
        if (isStop()) {
            return stop.getStopKname();
        }
        return route.getBrtId();
    }

    public String getDescription() {
        if (isStop()) {
            return "정류장 ID " + stop.getStopServiceid();
        }
        return route.getStartStop() + " -> " + route.getLastStop() + "\n" + route.getRemark();
    }
}
