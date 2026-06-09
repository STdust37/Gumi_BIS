package storage;

import model.RouteStop;
import parser.JsonObjectListParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 노선 정류장 목록 파일 캐시 저장 구현
 */
public class RouteStopCacheStore {
    private static final Path CACHE_PATH = Path.of("cache", "route-stops-cache.json");
    private static final String LIST_KEY = "routeStops";

    public HashMap<String, ArrayList<RouteStop>> load() {
        HashMap<String, ArrayList<RouteStop>> cache = new HashMap<>();

        // 캐시 파일이 없으면 빈 캐시 반환
        if (!Files.isRegularFile(CACHE_PATH)) {
            return cache;
        }

        try {
            // 파일에 저장된 JSON 문자열 읽기
            String json = Files.readString(CACHE_PATH, StandardCharsets.UTF_8);

            // routeStops 배열만 꺼내서 row 목록으로 파싱
            ArrayList<HashMap<String, String>> rows = JsonObjectListParser.parseList(json, LIST_KEY);
            for (HashMap<String, String> row : rows) {
                // HashMap row를 RouteStop 객체로 변환
                RouteStop routeStop = RouteStop.fromMap(row);

                // routeId, serviceId, stopName이 없는 데이터는 캐시에 넣지 않는 처리
                if (routeStop.getRouteId().isBlank()
                        || routeStop.getServiceId().isBlank()
                        || routeStop.getStopName().isBlank()) {
                    continue;
                }

                // routeId별로 정류장 목록을 나누어 저장
                ArrayList<RouteStop> routeStops = cache.get(routeStop.getRouteId());
                if (routeStops == null) {
                    routeStops = new ArrayList<>();
                    cache.put(routeStop.getRouteId(), routeStops);
                }
                routeStops.add(routeStop);
            }
        } catch (IOException ignored) {
            // 파일 읽기 실패 시 프로그램이 멈추지 않도록 빈 캐시 반환
            return new HashMap<>();
        }
        return cache;
    }

    public void save(HashMap<String, ArrayList<RouteStop>> cache) {
        try {
            // cache 폴더가 없으면 먼저 생성
            Files.createDirectories(CACHE_PATH.getParent());

            // 메모리에 있는 캐시 데이터를 JSON 문자열로 바꿔 저장
            Files.writeString(CACHE_PATH, toJson(cache), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // 캐시 저장 실패는 핵심 기능 실패가 아니므로 무시 처리
        }
    }

    private String toJson(HashMap<String, ArrayList<RouteStop>> cache) {
        // 외부 라이브러리 없이 JSON 문자열을 직접 조립하는 처리
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"version\": 1,\n");
        builder.append("  \"cachedAt\": \"").append(escape(LocalDateTime.now().toString())).append("\",\n");
        builder.append("  \"").append(LIST_KEY).append("\": [\n");

        boolean first = true;
        for (Map.Entry<String, ArrayList<RouteStop>> entry : cache.entrySet()) {
            for (RouteStop stop : entry.getValue()) {
                // 첫 번째 항목이 아니면 JSON 배열 구분 쉼표 추가
                if (!first) {
                    builder.append(",\n");
                }
                appendRouteStop(builder, stop);
                first = false;
            }
        }

        builder.append("\n  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendRouteStop(StringBuilder builder, RouteStop stop) {
        // RouteStop 객체 하나를 JSON 객체 하나로 변환
        builder.append("    {");
        appendField(builder, "brtId", stop.getBrtId(), true);
        appendField(builder, "routeId", stop.getRouteId(), false);
        appendField(builder, "linkId", stop.getLinkId(), false);
        appendField(builder, "brsSeqno", stop.getBrsSeqno(), false);
        appendField(builder, "stopName", stop.getStopName(), false);
        appendField(builder, "stopX", stop.getStopX(), false);
        appendField(builder, "stopY", stop.getStopY(), false);
        appendField(builder, "serviceId", stop.getServiceId(), false);
        builder.append("}");
    }

    private void appendField(StringBuilder builder, String key, String value, boolean first) {
        // 첫 번째 필드가 아니면 필드 구분 쉼표 추가
        if (!first) {
            builder.append(",");
        }
        builder.append("\"").append(key).append("\":\"").append(escape(value)).append("\"");
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        // JSON 문자열에서 문제가 되는 특수문자 변환
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                builder.append("\\\\");
            } else if (c == '"') {
                builder.append("\\\"");
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else if (c == '\t') {
                builder.append("\\t");
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
