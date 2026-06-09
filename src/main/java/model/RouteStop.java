package model;

import java.util.HashMap;

public class RouteStop {
    private final String brtId;
    private final String routeId;
    private final String linkId;
    private final String brsSeqno;
    private final String stopName;
    private final String stopX;
    private final String stopY;
    private final String serviceId;

    public RouteStop(String brtId, String routeId, String linkId, String brsSeqno, String stopName,
                     String stopX, String stopY, String serviceId) {
        this.brtId = brtId;
        this.routeId = routeId;
        this.linkId = linkId;
        this.brsSeqno = brsSeqno;
        this.stopName = stopName;
        this.stopX = stopX;
        this.stopY = stopY;
        this.serviceId = serviceId;
    }

    public static RouteStop fromMap(HashMap<String, String> map) {
        return new RouteStop(
                value(map, "brtId"),
                value(map, "routeId"),
                value(map, "linkId"),
                value(map, "brsSeqno"),
                value(map, "stopName"),
                value(map, "stopX"),
                value(map, "stopY"),
                value(map, "serviceId")
        );
    }

    public String getBrtId() {
        return brtId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getLinkId() {
        return linkId;
    }

    public String getBrsSeqno() {
        return brsSeqno;
    }

    public String getStopName() {
        return stopName;
    }

    public String getStopX() {
        return stopX;
    }

    public String getStopY() {
        return stopY;
    }

    public String getServiceId() {
        return serviceId;
    }

    private static String value(HashMap<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
