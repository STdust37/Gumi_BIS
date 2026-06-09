package service;

import client.BisClient;
import model.ArrivalInfo;
import model.BusLocation;
import model.BusStop;
import model.Route;
import model.RouteStop;
import storage.RouteStopCacheStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * BIS 데이터 로드와 검색 기능 연결 역할
 */
public class BisSearchService {
    private final BisClient client;
    private final RouteStopCacheStore routeStopCacheStore = new RouteStopCacheStore();
    private final ArrayList<Route> routes = new ArrayList<>();
    private final ArrayList<BusStop> stops = new ArrayList<>();
    private final HashMap<String, ArrayList<RouteStop>> routeStopCache = new HashMap<>();

    public BisSearchService(BisClient client) {
        this.client = client;
    }

    public void loadMasterData() throws IOException, InterruptedException {
        // 새로 로드하기 전에 이전 목록 초기화
        routes.clear();
        stops.clear();
        routeStopCache.clear();

        // BIS에서 전체 노선 목록 요청
        ArrayList<HashMap<String, String>> routeRows = client.selectRealtimeRouteList();
        for (HashMap<String, String> row : routeRows) {
            // HashMap row를 Route 객체로 변환
            Route route = Route.fromMap(row);

            // 검색에 필요한 routeId와 버스 번호가 있는 데이터만 저장
            if (!route.getRouteId().isBlank() && !route.getBrtId().isBlank()) {
                routes.add(route);
            }
        }

        // BIS에서 전체 정류장 목록 요청
        ArrayList<HashMap<String, String>> stopRows = client.selectRealtimeBusstopList();
        for (HashMap<String, String> row : stopRows) {
            // HashMap row를 BusStop 객체로 변환
            BusStop stop = BusStop.fromMap(row);

            // 검색에 필요한 정류장 ID와 이름이 있는 데이터만 저장
            if (!stop.getStopServiceid().isBlank() && !stop.getStopKname().isBlank()) {
                stops.add(stop);
            }
        }

        // 이전 실행에서 저장된 노선 정류장 캐시 로드
        loadRouteStopFileCache();
    }

    public boolean isMasterDataLoaded() {
        return !routes.isEmpty() && !stops.isEmpty();
    }

    public ArrayList<Route> getRoutes() {
        return routes;
    }

    public ArrayList<BusStop> getStops() {
        return stops;
    }

    public ArrayList<BusStop> searchStops(String keyword) {
        // 검색어 비교를 쉽게 하기 위한 소문자/공백 정리
        String term = normalize(keyword);
        ArrayList<BusStop> results = new ArrayList<>();

        // 검색어가 비어 있으면 빈 결과 반환
        if (term.isBlank()) {
            return results;
        }

        // 메모리에 저장된 전체 정류장 목록에서 직접 검색
        for (BusStop stop : stops) {
            String name = normalize(stop.getStopKname());
            String id = normalize(stop.getStopServiceid());

            // 이름 포함 검색 또는 ID 정확히 일치 검색
            if (name.contains(term) || id.equals(term)) {
                results.add(stop);
            }
        }
        return results;
    }

    public ArrayList<Route> searchRoutes(String keyword) {
        // 검색어 비교를 쉽게 하기 위한 소문자/공백 정리
        String term = normalize(keyword);
        ArrayList<Route> results = new ArrayList<>();

        // 검색어가 비어 있으면 빈 결과 반환
        if (term.isBlank()) {
            return results;
        }

        // 메모리에 저장된 전체 노선 목록에서 직접 검색
        for (Route route : routes) {
            String brtId = normalize(route.getBrtId());

            // 버스 번호가 검색어를 포함하면 결과에 추가
            if (brtId.contains(term)) {
                results.add(route);
            }
        }
        return results;
    }

    public ArrayList<RouteStop> getRouteStops(String routeId) throws IOException, InterruptedException {
        // 이미 메모리에 있는 노선 정류장 목록이면 바로 반환
        if (routeStopCache.containsKey(routeId)) {
            return routeStopCache.get(routeId);
        }

        // 메모리에 없으면 BIS에서 노선 정류장 목록 요청
        ArrayList<HashMap<String, String>> rows = client.getRealtimeRoute(routeId);
        ArrayList<RouteStop> routeStops = new ArrayList<>();
        for (HashMap<String, String> row : rows) {
            // HashMap row를 RouteStop 객체로 변환
            RouteStop routeStop = RouteStop.fromMap(row);

            // 정류장 ID와 이름이 있는 데이터만 노선도에 사용
            if (!routeStop.getServiceId().isBlank() && !routeStop.getStopName().isBlank()) {
                routeStops.add(routeStop);
            }
        }

        // 다음 조회를 빠르게 하기 위해 메모리 캐시에 저장
        routeStopCache.put(routeId, routeStops);

        // 프로그램 재실행 후에도 쓰기 위해 파일 캐시에 저장
        routeStopCacheStore.save(routeStopCache);
        return routeStops;
    }

    public ArrayList<ArrivalInfo> getArrivalInfo(String serviceId) throws IOException, InterruptedException {
        // 정류장 ID로 실시간 도착 정보 요청
        ArrayList<HashMap<String, String>> rows = client.getRealtimeArrivalInfo(serviceId);
        ArrayList<ArrivalInfo> arrivals = new ArrayList<>();
        for (HashMap<String, String> row : rows) {
            // HashMap row를 ArrivalInfo 객체로 변환
            ArrivalInfo arrival = ArrivalInfo.fromMap(row);

            // 노선 ID와 버스 번호가 있는 도착 정보만 표시
            if (!arrival.getRouteId().isBlank() && !arrival.getBrtId().isBlank()) {
                arrivals.add(arrival);
            }
        }
        return arrivals;
    }

    public ArrayList<HashMap<String, String>> getRouteShapePoints(String routeId) throws IOException, InterruptedException {
        return client.selectDrawRouteList(routeId);
    }

    public ArrayList<BusLocation> getBusLocations(String routeId) throws IOException, InterruptedException {
        // 노선 ID로 실시간 버스 위치 요청
        ArrayList<HashMap<String, String>> rows = client.getRealtimeBusLoc(routeId);
        ArrayList<BusLocation> locations = new ArrayList<>();
        for (HashMap<String, String> row : rows) {
            // HashMap row를 BusLocation 객체로 변환
            locations.add(BusLocation.fromMap(row));
        }
        return locations;
    }

    public ArrayList<Route> filterRoutesByDestination(ArrayList<Route> candidates, String destinationKeyword) throws IOException, InterruptedException {
        // 목적지 검색어 정리
        String destination = normalize(destinationKeyword);
        ArrayList<Route> filtered = new ArrayList<>();

        // 목적지가 비어 있으면 필터 결과 없음
        if (destination.isBlank()) {
            return filtered;
        }

        // 후보 노선마다 전체 정류장 순서를 확인
        for (Route route : candidates) {
            ArrayList<RouteStop> routeStops = getRouteStops(route.getRouteId());
            for (RouteStop routeStop : routeStops) {
                // 목적지 이름 포함 또는 정류장 ID 일치 여부 확인
                if (normalize(routeStop.getStopName()).contains(destination) || normalize(routeStop.getServiceId()).equals(destination)) {
                    filtered.add(route);
                    break;
                }
            }
        }
        return filtered;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private void loadRouteStopFileCache() {
        // 파일에 저장된 노선 정류장 캐시 읽기
        HashMap<String, ArrayList<RouteStop>> fileCache = routeStopCacheStore.load();
        if (fileCache.isEmpty()) {
            return;
        }

        // 현재 BIS에서 받은 노선 ID 목록 생성
        HashMap<String, Boolean> validRouteIds = new HashMap<>();
        for (Route route : routes) {
            validRouteIds.put(route.getRouteId(), true);
        }

        // 현재 존재하는 노선의 캐시만 메모리에 복원
        for (String routeId : fileCache.keySet()) {
            if (validRouteIds.containsKey(routeId)) {
                routeStopCache.put(routeId, fileCache.get(routeId));
            }
        }
    }
}
