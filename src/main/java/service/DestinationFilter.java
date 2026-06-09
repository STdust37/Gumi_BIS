package service;

import model.RouteStop;

import java.util.ArrayList;

/**
 * 도착지 키워드 기반 노선 필터링 구현
 */
public class DestinationFilter {
    public boolean routeContains(ArrayList<RouteStop> routeStops, String keyword) {
        // 비교하기 쉬운 소문자 검색어 준비
        String term = keyword == null ? "" : keyword.trim().toLowerCase();
        if (term.isBlank()) {
            return true;
        }

        // 노선에 포함된 정류장들을 하나씩 확인
        for (RouteStop stop : routeStops) {
            String stopName = stop.getStopName().toLowerCase();
            String serviceId = stop.getServiceId();

            // 정류장 이름 포함 또는 정류장 ID 일치 여부 확인
            if (stopName.contains(term) || serviceId.equals(term)) {
                return true;
            }
        }
        return false;
    }
}
