package service;

import model.BusStop;

import java.util.ArrayList;

/**
 * 정류장 이름과 ID 기반 검색 구현
 */
public class StopSearchService extends AbstractSearchService<BusStop> {
    private final ArrayList<BusStop> stops;

    public StopSearchService(ArrayList<BusStop> stops) {
        this.stops = new ArrayList<>(stops);
    }

    @Override
    protected ArrayList<BusStop> searchNormalized(String normalizedKeyword) {
        ArrayList<BusStop> results = new ArrayList<>();

        // 전체 정류장 목록을 처음부터 끝까지 순회
        for (BusStop stop : stops) {
            // 검색어와 비교할 정류장 이름, 정류장 ID 준비
            String stopName = normalize(stop.getStopKname());
            String stopId = normalize(stop.getStopServiceid());

            // 이름 포함 검색 또는 ID 완전 일치 검색
            if (stopName.contains(normalizedKeyword) || stopId.equals(normalizedKeyword)) {
                results.add(stop);
            }
        }
        return results;
    }
}
