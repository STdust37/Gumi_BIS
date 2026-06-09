package service;

import model.BusStop;
import model.HotPlace;
import model.NearbyStopCandidate;

import java.util.ArrayList;

/**
 * 좌표 거리 기반 가까운 정류장 검색 구현
 */
public class NearbyStopSearchService {
    private final DestinationStopResolver resolver = new DestinationStopResolver();

    public ArrayList<NearbyStopCandidate> findNearby(HotPlace hotPlace, ArrayList<BusStop> stops, int limit) {
        return resolver.resolveTop(hotPlace, stops, limit);
    }
}
