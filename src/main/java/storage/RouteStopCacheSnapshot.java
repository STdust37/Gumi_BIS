package storage;

import model.RouteStop;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 노선 정류장 캐시 스냅샷 모델
 */
public class RouteStopCacheSnapshot {
    private final HashMap<String, ArrayList<RouteStop>> routeStopsByRouteId;

    public RouteStopCacheSnapshot(HashMap<String, ArrayList<RouteStop>> routeStopsByRouteId) {
        // 노선별 정류장 캐시 목록 복사 저장
        this.routeStopsByRouteId = new HashMap<>(routeStopsByRouteId);
    }

    public HashMap<String, ArrayList<RouteStop>> getRouteStopsByRouteId() {
        // 내부 캐시 맵 보호를 위한 복사본 반환
        return new HashMap<>(routeStopsByRouteId);
    }

    public int routeCount() {
        // 캐시된 노선 개수 반환
        return routeStopsByRouteId.size();
    }
}
