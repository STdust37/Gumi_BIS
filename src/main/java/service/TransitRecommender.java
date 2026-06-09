package service;

import model.BusStop;
import model.HotPlace;
import model.HotPlaceTransitPlan;
import model.NearbyStopCandidate;

import java.io.IOException;

/**
 * 교통편 추천 서비스 계약 역할
 */
public interface TransitRecommender {
    NearbyStopCandidate resolvePrimaryDestination(HotPlace hotPlace);

    HotPlaceTransitPlan recommend(HotPlace hotPlace, BusStop departureStop) throws IOException, InterruptedException;
}
