package service;

import model.BusStop;
import model.RouteStop;

import java.util.ArrayList;

/**
 * 출발 정류장과 도착 정류장 후보 매칭 구현
 */
public class RouteStopMatcher {
    public int indexOf(ArrayList<RouteStop> routeStops, BusStop stop) {
        // 비교할 출발 정류장 ID와 이름 준비
        String stopId = normalize(stop.getStopServiceid());
        String stopName = normalize(stop.getStopKname());

        // 노선도 안의 정류장을 순서대로 비교
        for (int i = 0; i < routeStops.size(); i++) {
            RouteStop routeStop = routeStops.get(i);
            // ID 또는 이름이 같으면 해당 위치 반환
            if (normalize(routeStop.getServiceId()).equals(stopId)
                    || normalize(routeStop.getStopName()).equals(stopName)) {
                return i;
            }
        }
        return -1;
    }

    private String normalize(String value) {
        // null 입력과 대소문자 차이를 줄이기 위한 변환
        return value == null ? "" : value.trim().toLowerCase();
    }
}
