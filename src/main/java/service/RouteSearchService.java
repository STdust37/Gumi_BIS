package service;

import model.Route;

import java.util.ArrayList;

/**
 * 버스 노선 번호 기반 검색 구현
 */
public class RouteSearchService extends AbstractSearchService<Route> {
    private final ArrayList<Route> routes;

    public RouteSearchService(ArrayList<Route> routes) {
        this.routes = new ArrayList<>(routes);
    }

    @Override
    protected ArrayList<Route> searchNormalized(String normalizedKeyword) {
        ArrayList<Route> results = new ArrayList<>();

        // 전체 노선 목록을 순서대로 검사
        for (Route route : routes) {
            // 검색어와 비교할 버스 번호, 내부 노선 ID 준비
            String busNumber = normalize(route.getBrtId());
            String routeId = normalize(route.getRouteId());

            // 버스 번호 포함 검색 또는 routeId 완전 일치 검색
            if (busNumber.contains(normalizedKeyword) || routeId.equals(normalizedKeyword)) {
                results.add(route);
            }
        }
        return results;
    }
}
