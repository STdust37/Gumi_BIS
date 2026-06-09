package model;

import java.util.ArrayList;

public class HotPlaceTransitPlan {
    private final HotPlace hotPlace;
    private final BusStop departureStop;
    private final NearbyStopCandidate primaryDestinationStop;
    private final ArrayList<HotPlaceTransitOption> options;

    public HotPlaceTransitPlan(HotPlace hotPlace, BusStop departureStop,
                               NearbyStopCandidate primaryDestinationStop,
                               ArrayList<HotPlaceTransitOption> options) {
        this.hotPlace = hotPlace;
        this.departureStop = departureStop;
        this.primaryDestinationStop = primaryDestinationStop;
        this.options = options;
    }

    public HotPlace getHotPlace() {
        return hotPlace;
    }

    public BusStop getDepartureStop() {
        return departureStop;
    }

    public NearbyStopCandidate getPrimaryDestinationStop() {
        return primaryDestinationStop;
    }

    public ArrayList<HotPlaceTransitOption> getOptions() {
        return options;
    }
}
