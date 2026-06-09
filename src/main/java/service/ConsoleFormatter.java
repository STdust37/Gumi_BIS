package service;

import model.ArrivalInfo;
import model.BusLocation;
import model.BusStop;
import model.Route;
import model.RouteStop;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 콘솔 MVP 출력 형식 구성 역할
 * 검색 결과, 노선도, 도착 정보, 버스 위치를 사람이 읽기 쉬운 문장으로 변환
 */
public final class ConsoleFormatter {
    private ConsoleFormatter() {
        // 객체 생성을 막기 위한 private 생성자
    }

    public static void printStopSearchResults(ArrayList<BusStop> stops, String keyword, int limit) {
        // 정류장 검색 결과 출력 시작
        System.out.println("==== 정류장(" + stops.size() + ") ====");
        if (stops.isEmpty()) {
            // 검색 결과 없음 안내
            System.out.println("검색 결과가 없습니다.");
            return;
        }

        // 출력 제한을 반영한 표시 개수 계산
        int count = Math.min(stops.size(), limit);
        for (int i = 0; i < count; i++) {
            // 현재 순번에 해당하는 정류장 선택
            BusStop stop = stops.get(i);
            // 검색어 포함 부분을 대괄호로 표시
            System.out.printf("%2d. %s%n", i + 1, markKeyword(stop.getStopKname(), keyword));
            // 정류장 ID와 항목 종류 출력
            System.out.printf("    %s | 정류장%n", stop.getStopServiceid());
        }
        // 생략된 결과 개수 안내
        printLimitNotice(stops.size(), limit);
    }

    public static void printRouteSearchResults(ArrayList<Route> routes, int limit) {
        // 버스 노선 검색 결과 출력 시작
        System.out.println("==== 버스(" + routes.size() + ") ====");
        if (routes.isEmpty()) {
            // 검색 결과 없음 안내
            System.out.println("검색 결과가 없습니다.");
            return;
        }

        // 출력 제한을 반영한 표시 개수 계산
        int count = Math.min(routes.size(), limit);
        for (int i = 0; i < count; i++) {
            // 현재 순번에 해당하는 노선 선택
            Route route = routes.get(i);
            // 노선 종류와 버스 번호 출력
            System.out.printf("%2d. [%s] %s%n", i + 1, routeType(route), route.getBrtId());
            // 기점, 종점, 운행 수, 내부 routeId 출력
            System.out.printf("    %s ↔ %s / 운행중=%s / routeId=%s%n",
                    emptyToDash(route.getStartStop()),
                    emptyToDash(route.getLastStop()),
                    emptyToDash(route.getRunningCount()),
                    emptyToDash(route.getRouteId()));
        }
        // 생략된 결과 개수 안내
        printLimitNotice(routes.size(), limit);
    }

    public static void printRoutes(ArrayList<Route> routes, int limit) {
        // 노선 목록 출력 메서드 재사용
        printRouteSearchResults(routes, limit);
    }

    public static void printStops(ArrayList<BusStop> stops, int limit) {
        // 정류장 목록 출력 메서드 재사용
        printStopSearchResults(stops, "", limit);
    }

    public static void printRouteStops(ArrayList<RouteStop> routeStops, String highlightStopId, int limit) {
        // 버스 위치 없이 노선 정류장 순서만 출력
        printRouteTimeline(routeStops, highlightStopId, new ArrayList<>(), limit);
    }

    public static void printRouteTimeline(ArrayList<RouteStop> routeStops, String highlightStopId,
                                          ArrayList<BusLocation> locations, int limit) {
        // 노선도 정류장 순서 출력 시작
        System.out.println("==== 노선 정류장 순서 (" + routeStops.size() + "개) ====");
        if (routeStops.isEmpty()) {
            // 노선 정류장 정보 없음 안내
            System.out.println("정류장 정보가 없습니다.");
            return;
        }

        // 실시간 버스 위치를 정류장 순번별로 묶는 처리
        HashMap<String, ArrayList<BusLocation>> busesBySeq = groupBusLocationsBySeq(locations);
        // 출력 제한을 반영한 표시 개수 계산
        int count = Math.min(routeStops.size(), limit);
        for (int i = 0; i < count; i++) {
            // 현재 순번에 해당하는 노선 정류장 선택
            RouteStop routeStop = routeStops.get(i);
            String selected = routeStop.getServiceId().equals(highlightStopId) ? " *선택*" : "";
            // 정류장 순번, 이름, ID 출력
            System.out.printf("%2d. [%s] %s (%s)%s%n",
                    i + 1,
                    emptyToDash(routeStop.getBrsSeqno()),
                    emptyToDash(routeStop.getStopName()),
                    emptyToDash(routeStop.getServiceId()),
                    selected);

            // 현재 정류장 순번에 매핑된 버스 위치 목록 조회
            ArrayList<BusLocation> buses = busesBySeq.get(routeStop.getBrsSeqno());
            if (buses != null) {
                // 같은 정류장 순번에 있는 버스들을 모두 출력
                for (BusLocation bus : buses) {
                    System.out.printf("    └ 버스 차량=%s, 남은정류장=%s, 남은시간=%s분%n",
                            emptyToDash(bus.getBidNo()),
                            emptyToInfoNone(bus.getRemainStop()),
                            emptyToInfoNone(bus.getRemainTime()));
                }
            }
        }
        // 생략된 결과 개수 안내
        printLimitNotice(routeStops.size(), limit);
    }

    public static void printArrivals(ArrayList<ArrivalInfo> arrivals, int limit) {
        // 정류장 도착 정보 출력 시작
        System.out.println("==== 정류장 버스 도착/운행 정보 (" + arrivals.size() + "개) ====");
        if (arrivals.isEmpty()) {
            // 도착 정보 없음 안내
            System.out.println("운행 정보가 없습니다.");
            return;
        }

        // 출력 제한을 반영한 표시 개수 계산
        int count = Math.min(arrivals.size(), limit);
        for (int i = 0; i < count; i++) {
            // 현재 순번에 해당하는 도착 정보 선택
            ArrivalInfo arrival = arrivals.get(i);
            // 버스 번호, 방면, 도착 예정 정보 출력
            System.out.printf("%2d. %s | %s 방면 | %s%n",
                    i + 1,
                    arrival.getBrtId(),
                    emptyToDash(arrival.getLastStop()),
                    arrivalText(arrival));
            // 노선 상세 조회에 필요한 내부 식별자 출력
            System.out.printf("    routeId=%s / %s -> %s / busStd=%s%n",
                    emptyToDash(arrival.getRouteId()),
                    emptyToDash(arrival.getStartStop()),
                    emptyToDash(arrival.getLastStop()),
                    emptyToDash(arrival.getBusStandardid()));
        }
        // 생략된 결과 개수 안내
        printLimitNotice(arrivals.size(), limit);
    }

    public static void printBusLocations(ArrayList<BusLocation> locations) {
        // 실시간 버스 위치 출력 시작
        System.out.println("==== 실시간 버스 위치 (" + locations.size() + "대) ====");
        if (locations.isEmpty()) {
            // 운행 중인 버스 위치 없음 안내
            System.out.println("현재 운행 중인 버스 위치 정보가 없습니다.");
            return;
        }

        // 버스 위치 목록을 순서대로 출력
        for (int i = 0; i < locations.size(); i++) {
            // 현재 순번에 해당하는 버스 위치 선택
            BusLocation location = locations.get(i);
            // 차량 번호, 현재 위치, 남은 정류장, 남은 시간 출력
            System.out.printf("%2d. 차량=%s busStd=%s 현재/도착정류장=%s seq=%s 남은정류장=%s 남은시간=%s분 색상=%s%n",
                    i + 1,
                    emptyToDash(location.getBidNo()),
                    emptyToDash(location.getBusStandardid()),
                    emptyToDash(location.getStopKname()),
                    emptyToDash(location.getBrsSeqno()),
                    emptyToInfoNone(location.getRemainStop()),
                    emptyToInfoNone(location.getRemainTime()),
                    emptyToDash(location.getBusColorType()));
        }
    }

    private static HashMap<String, ArrayList<BusLocation>> groupBusLocationsBySeq(ArrayList<BusLocation> locations) {
        // key: 정류장 순번, value: 해당 순번에 있는 버스 목록
        HashMap<String, ArrayList<BusLocation>> grouped = new HashMap<>();

        // 버스 위치를 정류장 순번별로 묶기 위한 반복
        for (BusLocation location : locations) {
            // 정류장 순번이 없는 데이터는 위치 표시에서 제외
            if (location.getBrsSeqno() == null || location.getBrsSeqno().isBlank()) {
                continue;
            }

            // computeIfAbsent 대신 직접 목록 생성과 추가 처리
            ArrayList<BusLocation> group = grouped.get(location.getBrsSeqno());
            if (group == null) {
                // 처음 등장한 정류장 순번이면 새 목록 생성
                group = new ArrayList<>();
                grouped.put(location.getBrsSeqno(), group);
            }
            // 해당 정류장 순번 목록에 버스 위치 추가
            group.add(location);
        }
        return grouped;
    }

    private static String routeType(Route route) {
        // 노선 설명과 방향 문자열을 합쳐 노선 종류 판단
        String text = (route.getRemark() + " " + route.getBrtDirection()).toLowerCase();
        if (text.contains("좌석")) {
            return "좌석";
        }
        // 좌석 키워드가 없으면 일반 노선으로 표시
        return "일반";
    }

    private static String arrivalText(ArrivalInfo arrival) {
        // 남은 정류장 정보가 없으면 도착 정보 없음으로 표시
        if ("정보없음".equals(arrival.getRemainStop()) || arrival.getRemainStop().isBlank()) {
            return "도착 정보 없음";
        }
        // 남은 시간과 몇 번째 전 정보를 조합
        return "약 " + emptyToInfoNone(arrival.getRemainTime()) + "분 [" + arrival.getRemainStop() + "번째 전]";
    }

    private static String markKeyword(String value, String keyword) {
        // 검색어 강조가 불가능한 경우 원래 값 반환
        if (value == null || keyword == null || keyword.isBlank()) {
            return emptyToDash(value);
        }

        // 대소문자 차이 없이 검색어 위치 확인
        String lowerValue = value.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerValue.indexOf(lowerKeyword);
        if (index < 0) {
            return value;
        }
        // 검색어가 포함된 부분을 대괄호로 감싼 문자열 생성
        int end = index + keyword.length();
        return value.substring(0, index) + "[" + value.substring(index, end) + "]" + value.substring(end);
    }

    private static void printLimitNotice(int total, int limit) {
        // 출력 제한보다 전체 결과가 많을 때 생략 안내 출력
        if (total > limit) {
            System.out.println("... " + (total - limit) + "개 더 있음. 더 좁은 검색어를 입력하세요.");
        }
    }

    private static String emptyToInfoNone(String value) {
        // 값이 없을 때 콘솔에 표시할 기본 문구 변환
        if (value == null || value.isBlank()) {
            return "정보없음";
        }
        return value;
    }

    private static String emptyToDash(String value) {
        // 값이 없을 때 콘솔 표시에 사용할 대시 변환
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
