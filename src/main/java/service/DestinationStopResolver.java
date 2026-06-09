package service;

import model.BusStop;
import model.HotPlace;
import model.NearbyStopCandidate;
import util.GeoUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * 장소 좌표 기반 주변 정류장 탐색 구현
 */
public class DestinationStopResolver {
    private final HashMap<String, ArrayList<NearbyStopCandidate>> nearbyStopCache = new HashMap<>();

    public NearbyStopCandidate resolvePrimary(HotPlace hotPlace, ArrayList<BusStop> stops) {
        // 가장 가까운 정류장 하나만 사용하는 대표값 조회
        ArrayList<NearbyStopCandidate> candidates = resolveTop(hotPlace, stops, 1);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public ArrayList<NearbyStopCandidate> resolveTop(HotPlace hotPlace, ArrayList<BusStop> stops, int limit) {
        // 같은 핫플을 다시 조회할 때 사용할 메모리 캐시 확인
        ArrayList<NearbyStopCandidate> cached = nearbyStopCache.get(cacheKey(hotPlace));
        if (cached != null) {
            return first(cached, limit);
        }

        ArrayList<NearbyStopCandidate> candidates = new ArrayList<>();

        // 핫플 좌표 문자열을 계산 가능한 좌표 객체로 변환
        GeoUtils.Coordinate placeCoordinate = GeoUtils.coordinate(hotPlace.getLongitude(), hotPlace.getLatitude());
        if (!placeCoordinate.isValid()) {
            return candidates;
        }

        // 모든 정류장과 핫플 사이의 직선거리 계산
        for (BusStop stop : stops) {
            GeoUtils.Coordinate stopCoordinate = GeoUtils.coordinate(stop.getStopX(), stop.getStopY());
            double distanceMeters = GeoUtils.distanceMeters(placeCoordinate, stopCoordinate);
            if (Double.isFinite(distanceMeters)) {
                candidates.add(new NearbyStopCandidate(stop, distanceMeters));
            }
        }

        sortByDistance(candidates);
        nearbyStopCache.put(cacheKey(hotPlace), candidates);
        return first(candidates, limit);
    }

    private ArrayList<NearbyStopCandidate> first(ArrayList<NearbyStopCandidate> candidates, int limit) {
        // 필요한 개수만 앞에서부터 복사
        ArrayList<NearbyStopCandidate> result = new ArrayList<>();
        for (NearbyStopCandidate candidate : candidates) {
            result.add(candidate);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private void sortByDistance(ArrayList<NearbyStopCandidate> candidates) {
        // 거리 오름차순 정렬 기준 구현
        candidates.sort(new Comparator<NearbyStopCandidate>() {
            @Override
            public int compare(NearbyStopCandidate left, NearbyStopCandidate right) {
                return Double.compare(left.getDistanceMeters(), right.getDistanceMeters());
            }
        });
    }

    private String cacheKey(HotPlace hotPlace) {
        // ID가 있으면 ID 기준 캐시 키 사용
        if (hotPlace.getId() != null && !hotPlace.getId().isBlank()) {
            return "id:" + hotPlace.getId();
        }

        // ID가 없으면 이름과 좌표를 합친 캐시 키 사용
        return "place:" + hotPlace.getName() + ":" + hotPlace.getLongitude() + ":" + hotPlace.getLatitude();
    }
}
