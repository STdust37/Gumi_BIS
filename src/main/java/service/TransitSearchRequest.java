package service;

import model.BusStop;
import model.HotPlace;

/**
 * 핫플 교통편 추천 요청값 모델
 */
public class TransitSearchRequest {
    private final HotPlace hotPlace;
    private final BusStop departureStop;

    public TransitSearchRequest(HotPlace hotPlace, BusStop departureStop) {
        this.hotPlace = hotPlace;
        this.departureStop = departureStop;
    }

    public HotPlace getHotPlace() {
        return hotPlace;
    }

    public BusStop getDepartureStop() {
        return departureStop;
    }
}
