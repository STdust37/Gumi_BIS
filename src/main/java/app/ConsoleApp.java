package app;

import client.BisClient;
import model.ArrivalInfo;
import model.BusLocation;
import model.BusStop;
import model.Route;
import model.RouteStop;
import service.BisSearchService;
import service.ConsoleFormatter;
import service.FavoriteStore;
import service.RecentSearchStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

public class ConsoleApp {
    private final Scanner scanner;
    private final BisSearchService service;
    private final FavoriteStore favoriteStore;
    private final RecentSearchStore recentSearchStore;
    private boolean exitRequested;

    public ConsoleApp(Scanner scanner, BisSearchService service, FavoriteStore favoriteStore, RecentSearchStore recentSearchStore) {
        this.scanner = scanner;
        this.service = service;
        this.favoriteStore = favoriteStore;
        this.recentSearchStore = recentSearchStore;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BisClient client = new BisClient();
        BisSearchService service = new BisSearchService(client);
        ConsoleApp app = new ConsoleApp(scanner, service, new FavoriteStore(), new RecentSearchStore());
        app.run();
    }

    private void run() {
        println("구미 BIS 콘솔 검증 MVP");
        println("데이터를 불러오는 중입니다...");
        try {
            service.loadMasterData();
            printMasterSummary();
        } catch (Exception e) {
            println("초기 데이터 로드 실패: " + e.getMessage());
            println("네트워크 상태나 BIS 사이트 응답을 확인한 뒤 메뉴에서 1번으로 다시 시도하세요.");
        }

        while (true) {
            printMenu();
            String choice = prompt("메뉴 선택");
            if (exitRequested) {
                println("종료합니다.");
                return;
            }
            try {
                if ("0".equals(choice)) {
                    println("종료합니다.");
                    return;
                } else if ("1".equals(choice)) {
                    reloadMasterData();
                } else if ("2".equals(choice)) {
                    unifiedSearchFlow();
                } else if ("3".equals(choice)) {
                    favoritesFlow();
                } else if ("4".equals(choice)) {
                    printRecentSearches();
                } else {
                    println("알 수 없는 메뉴입니다.");
                }
            } catch (Exception e) {
                println("처리 중 오류: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        println("");
        println("==== 메뉴 ====");
        println("1. 전체 노선/정류장 데이터 다시 불러오기");
        println("2. 통합 검색(버스/정류장)");
        println("3. 즐겨찾기 보기/관리");
        println("4. 최근 검색 기록 보기");
        println("0. 종료");
    }

    private void reloadMasterData() throws IOException, InterruptedException {
        println("데이터를 다시 불러오는 중입니다...");
        service.loadMasterData();
        printMasterSummary();
    }

    private void printMasterSummary() {
        println("노선 " + service.getRoutes().size() + "개, 정류장 " + service.getStops().size() + "개를 불러왔습니다.");
    }

    private void unifiedSearchFlow() throws IOException, InterruptedException {
        ensureMasterData();
        String keyword = prompt("정류장 이름/ID 또는 버스 번호");
        if (keyword.isBlank()) {
            println("검색어를 입력하세요.");
            return;
        }

        recentSearchStore.add("검색:" + keyword);
        ArrayList<Route> routes = service.searchRoutes(keyword);
        ArrayList<BusStop> stops = service.searchStops(keyword);
        int total = routes.size() + stops.size();

        println("");
        println("관련 항목 " + total + "개 표시됨");
        println("버스(" + routes.size() + ") / 정류장(" + stops.size() + ")");
        if (total == 0) {
            println("검색 결과가 없습니다. 다시 시도해주세요.");
            return;
        }

        String category = routes.isEmpty() && !stops.isEmpty() ? "STOP" : "ROUTE";
        while (true) {
            if ("ROUTE".equals(category)) {
                ConsoleFormatter.printRouteSearchResults(routes, 30);
                String raw = prompt("버스 항목 번호 선택, t=정류장 보기, -1=뒤로가기");
                if (handleBack(raw)) return;
                if ("t".equalsIgnoreCase(raw)) {
                    category = "STOP";
                    continue;
                }
                int index = parseIndex(raw, routes.size());
                if (index >= 0) {
                    Route selected = routes.get(index);
                    showRouteDetail(selected.getRouteId(), selected.getBrtId(), null);
                }
            } else {
                ConsoleFormatter.printStopSearchResults(stops, keyword, 30);
                String raw = prompt("정류장 항목 번호 선택, t=버스 보기, -1=뒤로가기");
                if (handleBack(raw)) return;
                if ("t".equalsIgnoreCase(raw)) {
                    category = "ROUTE";
                    continue;
                }
                int index = parseIndex(raw, stops.size());
                if (index >= 0) {
                    showStopDetail(stops.get(index));
                }
            }
        }
    }

    private void showStopDetail(BusStop stop) throws IOException, InterruptedException {
        String sortMode = "ARRIVAL";
        String destinationFilter = "";
        ArrayList<ArrivalInfo> arrivals = service.getArrivalInfo(stop.getStopServiceid());

        while (true) {
            ArrayList<ArrivalInfo> visibleArrivals = destinationFilter.isBlank()
                    ? new ArrayList<>(arrivals)
                    : filterArrivalsByDestination(arrivals, destinationFilter);
            sortArrivals(visibleArrivals, sortMode);

            println("");
            println("정류장: " + stop.getStopKname() + " / ID " + stop.getStopServiceid());
            if (!destinationFilter.isBlank()) {
                println(stop.getStopKname() + " -> " + destinationFilter + " 가는 버스");
            }
            println("도착 정보 " + visibleArrivals.size() + "개 표시");
            ConsoleFormatter.printArrivals(visibleArrivals, 50);

            println("");
            println("옵션: 번호=노선 상세, s=정렬 변경(" + sortLabel(sortMode) + "), f=도착지 필터, c=필터 해제, a=즐겨찾기 토글, r=새로고침, -1=뒤로가기");
            String raw = prompt("선택");
            if (handleBack(raw)) return;
            if ("s".equalsIgnoreCase(raw)) {
                sortMode = "ARRIVAL".equals(sortMode) ? "NUMBER" : "ARRIVAL";
                continue;
            }
            if ("f".equalsIgnoreCase(raw)) {
                destinationFilter = prompt("도착지 정류장 이름/ID");
                continue;
            }
            if ("c".equalsIgnoreCase(raw)) {
                destinationFilter = "";
                continue;
            }
            if ("a".equalsIgnoreCase(raw)) {
                toggleFavorite("STOP", stop.getStopServiceid(), stop.getStopKname());
                continue;
            }
            if ("r".equalsIgnoreCase(raw)) {
                arrivals = service.getArrivalInfo(stop.getStopServiceid());
                println("도착 정보를 새로고침했습니다.");
                continue;
            }

            int index = parseIndex(raw, visibleArrivals.size());
            if (index >= 0) {
                ArrivalInfo arrival = visibleArrivals.get(index);
                showRouteDetail(arrival.getRouteId(), arrival.getBrtId(), stop.getStopServiceid());
            }
        }
    }

    private void showRouteDetail(String routeId, String brtId, String highlightStopId) throws IOException, InterruptedException {
        while (true) {
            println("");
            println("노선 상세: " + brtId + " / routeId=" + routeId);

            ArrayList<RouteStop> routeStops = service.getRouteStops(routeId);
            ArrayList<HashMap<String, String>> shapePoints = service.getRouteShapePoints(routeId);
            ArrayList<BusLocation> locations = service.getBusLocations(routeId);

            ConsoleFormatter.printRouteTimeline(routeStops, highlightStopId, locations, 100);
            println("노선도 좌표 수: " + shapePoints.size());
            ConsoleFormatter.printBusLocations(locations);

            println("");
            println("옵션: 번호=정류장 도착정보, a=즐겨찾기 토글, r=새로고침, -1=뒤로가기");
            String raw = prompt("선택");
            if (handleBack(raw)) return;
            if ("a".equalsIgnoreCase(raw)) {
                toggleFavorite("ROUTE", routeId, brtId + " 노선");
                continue;
            }
            if ("r".equalsIgnoreCase(raw)) {
                println("노선 정보를 새로고침합니다.");
                continue;
            }

            int index = parseIndex(raw, routeStops.size());
            if (index >= 0) {
                RouteStop routeStop = routeStops.get(index);
                BusStop stop = new BusStop(routeStop.getServiceId(), routeStop.getStopName(), routeStop.getStopX(), routeStop.getStopY());
                showStopDetail(stop);
            }
        }
    }

    private ArrayList<ArrivalInfo> filterArrivalsByDestination(ArrayList<ArrivalInfo> arrivals, String keyword)
            throws IOException, InterruptedException {
        String term = normalize(keyword);
        ArrayList<ArrivalInfo> results = new ArrayList<>();
        for (ArrivalInfo arrival : arrivals) {
            ArrayList<RouteStop> routeStops = service.getRouteStops(arrival.getRouteId());
            for (RouteStop routeStop : routeStops) {
                if (normalize(routeStop.getStopName()).contains(term) || normalize(routeStop.getServiceId()).equals(term)) {
                    results.add(arrival);
                    break;
                }
            }
        }
        return results;
    }

    private void sortArrivals(ArrayList<ArrivalInfo> arrivals, String sortMode) {
        if ("NUMBER".equals(sortMode)) {
            arrivals.sort(Comparator.comparing(ArrivalInfo::getBrtId, this::compareRouteNumbers));
            return;
        }
        arrivals.sort(Comparator
                .comparingInt(this::arrivalSortValue)
                .thenComparing(ArrivalInfo::getBrtId, this::compareRouteNumbers));
    }

    private int arrivalSortValue(ArrivalInfo arrival) {
        int seconds = parseIntOrDefault(arrival.getRemainTimeSec(), -1);
        if (seconds >= 0) {
            return seconds;
        }

        int minutes = parseIntOrDefault(arrival.getRemainTime(), -1);
        if (minutes >= 0) {
            return minutes * 60;
        }
        return Integer.MAX_VALUE;
    }

    private int compareRouteNumbers(String left, String right) {
        int leftNumber = firstNumber(left);
        int rightNumber = firstNumber(right);
        if (leftNumber != rightNumber) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return left.compareToIgnoreCase(right);
    }

    private int firstNumber(String value) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (number.length() > 0) {
                break;
            }
        }
        if (number.length() == 0) {
            return Integer.MAX_VALUE;
        }
        return parseIntOrDefault(number.toString(), Integer.MAX_VALUE);
    }

    private void favoritesFlow() throws IOException, InterruptedException {
        while (true) {
            ArrayList<FavoriteEntry> favorites = favoriteEntries();
            println("");
            println("==== 즐겨찾기 ====");
            if (favorites.isEmpty()) {
                println("없음");
            } else {
                for (int i = 0; i < favorites.size(); i++) {
                    FavoriteEntry entry = favorites.get(i);
                    println((i + 1) + ". [" + entry.displayType + "] " + entry.label + " / " + entry.id);
                }
            }

            println("옵션: 번호=열기, d번호=삭제, -1=뒤로가기");
            String raw = prompt("선택");
            if (handleBack(raw)) return;
            if (raw.startsWith("d") || raw.startsWith("D")) {
                int index = parseIndex(raw.substring(1).trim(), favorites.size());
                if (index >= 0) {
                    FavoriteEntry entry = favorites.get(index);
                    favoriteStore.remove(entry.type, entry.id);
                    println("즐겨찾기에서 제거했습니다.");
                }
                continue;
            }

            int index = parseIndex(raw, favorites.size());
            if (index >= 0) {
                openFavorite(favorites.get(index));
            }
        }
    }

    private ArrayList<FavoriteEntry> favoriteEntries() {
        ArrayList<FavoriteEntry> entries = new ArrayList<>();
        HashMap<String, ArrayList<String>> snapshot = favoriteStore.snapshot();
        addFavoriteEntries(entries, "STOP", "정류장", snapshot.get("STOP"));
        addFavoriteEntries(entries, "ROUTE", "노선", snapshot.get("ROUTE"));
        return entries;
    }

    private void addFavoriteEntries(ArrayList<FavoriteEntry> entries, String type, String displayType, ArrayList<String> rows) {
        if (rows == null) {
            return;
        }
        for (String row : rows) {
            String[] parts = row.split("\\|", 2);
            String id = parts.length > 0 ? parts[0].trim() : "";
            String label = parts.length > 1 ? parts[1].trim() : row;
            entries.add(new FavoriteEntry(type, displayType, id, label));
        }
    }

    private void openFavorite(FavoriteEntry entry) throws IOException, InterruptedException {
        if ("STOP".equals(entry.type)) {
            BusStop stop = findStopById(entry.id);
            if (stop == null) {
                println("해당 정류장을 찾을 수 없습니다: " + entry.id);
                return;
            }
            showStopDetail(stop);
            return;
        }

        Route route = findRouteById(entry.id);
        if (route == null) {
            println("해당 노선을 찾을 수 없습니다: " + entry.id);
            return;
        }
        showRouteDetail(route.getRouteId(), route.getBrtId(), null);
    }

    private BusStop findStopById(String serviceId) {
        for (BusStop stop : service.getStops()) {
            if (stop.getStopServiceid().equals(serviceId)) {
                return stop;
            }
        }
        return null;
    }

    private Route findRouteById(String routeId) {
        for (Route route : service.getRoutes()) {
            if (route.getRouteId().equals(routeId)) {
                return route;
            }
        }
        return null;
    }

    private void printRecentSearches() {
        println("==== 최근 검색 ====");
        ArrayList<String> history = recentSearchStore.snapshot();
        if (history.isEmpty()) {
            println("없음");
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            println((i + 1) + ". " + history.get(i));
        }
    }

    private void toggleFavorite(String type, String id, String label) {
        boolean isFavorite = favoriteStore.toggle(type, id, label);
        println(isFavorite ? "즐겨찾기에 추가했습니다." : "즐겨찾기에서 제거했습니다.");
    }

    private void ensureMasterData() throws IOException, InterruptedException {
        if (!service.isMasterDataLoaded()) {
            service.loadMasterData();
            printMasterSummary();
        }
    }

    private boolean handleBack(String raw) {
        return "-1".equals(raw);
    }

    private int parseIndex(String raw, int size) {
        try {
            int value = Integer.parseInt(raw);
            int index = value - 1;
            if (index >= 0 && index < size) {
                return index;
            }
        } catch (NumberFormatException ignored) {
        }
        println("1부터 " + size + " 사이 숫자 또는 -1을 입력하세요.");
        return -1;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String sortLabel(String sortMode) {
        return "NUMBER".equals(sortMode) ? "번호순" : "도착순";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private String prompt(String message) {
        System.out.print(message + "> ");
        if (!scanner.hasNextLine()) {
            exitRequested = true;
            return "";
        }
        return scanner.nextLine().trim();
    }

    private void println(String message) {
        System.out.println(message);
    }

    private static final class FavoriteEntry {
        private final String type;
        private final String displayType;
        private final String id;
        private final String label;

        private FavoriteEntry(String type, String displayType, String id, String label) {
            this.type = type;
            this.displayType = displayType;
            this.id = id;
            this.label = label;
        }
    }
}
