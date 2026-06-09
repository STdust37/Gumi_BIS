package model;

import java.util.HashMap;

public class ArrivalInfo {
    private final String brtId;
    private final String routeId;
    private final String startStop;
    private final String lastStop;
    private final String remainStop;
    private final String remainTime;
    private final String remainTimeSec;
    private final String busStandardid;
    private final String brtStandardid;

    public ArrivalInfo(String brtId, String routeId, String startStop, String lastStop, String remainStop,
                       String remainTime, String remainTimeSec, String busStandardid, String brtStandardid) {
        this.brtId = brtId;
        this.routeId = routeId;
        this.startStop = startStop;
        this.lastStop = lastStop;
        this.remainStop = remainStop;
        this.remainTime = remainTime;
        this.remainTimeSec = remainTimeSec;
        this.busStandardid = busStandardid;
        this.brtStandardid = brtStandardid;
    }

    public static ArrivalInfo fromMap(HashMap<String, String> map) {
        return new ArrivalInfo(
                value(map, "brtId"),
                value(map, "routeId"),
                value(map, "startStop"),
                value(map, "lastStop"),
                value(map, "remainStop"),
                value(map, "remainTime"),
                value(map, "remainTimeSec"),
                value(map, "busStandardid"),
                value(map, "brtStandardid")
        );
    }

    public String getBrtId() {
        return brtId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getStartStop() {
        return startStop;
    }

    public String getLastStop() {
        return lastStop;
    }

    public String getRemainStop() {
        return remainStop;
    }

    public String getRemainTime() {
        return remainTime;
    }

    public String getRemainTimeSec() {
        return remainTimeSec;
    }

    public String getBusStandardid() {
        return busStandardid;
    }

    public String getBrtStandardid() {
        return brtStandardid;
    }

    private static String value(HashMap<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
