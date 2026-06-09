package model;

import java.util.HashMap;

public class BusLocation {
    private final String routeId;
    private final String brtId;
    private final String bidNo;
    private final String busStandardid;
    private final String stopKname;
    private final String brsSeqno;
    private final String remainStop;
    private final String remainTime;
    private final String remainTimeSec;
    private final String busColorType;

    public BusLocation(String routeId, String brtId, String bidNo, String busStandardid, String stopKname,
                       String brsSeqno, String remainStop, String remainTime, String remainTimeSec,
                       String busColorType) {
        this.routeId = routeId;
        this.brtId = brtId;
        this.bidNo = bidNo;
        this.busStandardid = busStandardid;
        this.stopKname = stopKname;
        this.brsSeqno = brsSeqno;
        this.remainStop = remainStop;
        this.remainTime = remainTime;
        this.remainTimeSec = remainTimeSec;
        this.busColorType = busColorType;
    }

    public static BusLocation fromMap(HashMap<String, String> map) {
        return new BusLocation(
                value(map, "routeId"),
                value(map, "brtId"),
                value(map, "bidNo"),
                value(map, "busStandardid"),
                value(map, "stopKname"),
                value(map, "brsSeqno"),
                value(map, "remainStop"),
                value(map, "remainTime"),
                value(map, "remainTimeSec"),
                value(map, "busColorType")
        );
    }

    public String getRouteId() {
        return routeId;
    }

    public String getBrtId() {
        return brtId;
    }

    public String getBidNo() {
        return bidNo;
    }

    public String getBusStandardid() {
        return busStandardid;
    }

    public String getStopKname() {
        return stopKname;
    }

    public String getBrsSeqno() {
        return brsSeqno;
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

    public String getBusColorType() {
        return busColorType;
    }

    private static String value(HashMap<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
