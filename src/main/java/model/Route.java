package model;

import java.util.HashMap;

public class Route {
    private final String routeId;
    private final String brtStandardid;
    private final String brtId;
    private final String brtDirection;
    private final String startStop;
    private final String lastStop;
    private final String remark;
    private final String runningCount;

    public Route(String routeId, String brtStandardid, String brtId, String brtDirection, String startStop,
                 String lastStop, String remark, String runningCount) {
        this.routeId = routeId;
        this.brtStandardid = brtStandardid;
        this.brtId = brtId;
        this.brtDirection = brtDirection;
        this.startStop = startStop;
        this.lastStop = lastStop;
        this.remark = remark;
        this.runningCount = runningCount;
    }

    public static Route fromMap(HashMap<String, String> map) {
        return new Route(
                value(map, "routeId"),
                value(map, "brtStandardid"),
                value(map, "brtId"),
                value(map, "brtDirection"),
                value(map, "startStop"),
                value(map, "lastStop"),
                value(map, "remark"),
                value(map, "cnt")
        );
    }

    public String getRouteId() {
        return routeId;
    }

    public String getBrtStandardid() {
        return brtStandardid;
    }

    public String getBrtId() {
        return brtId;
    }

    public String getBrtDirection() {
        return brtDirection;
    }

    public String getStartStop() {
        return startStop;
    }

    public String getLastStop() {
        return lastStop;
    }

    public String getRemark() {
        return remark;
    }

    public String getRunningCount() {
        return runningCount;
    }

    private static String value(HashMap<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
