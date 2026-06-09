package model;

import java.util.HashMap;

public class BusStop {
    private final String stopServiceid;
    private final String stopKname;
    private final String stopX;
    private final String stopY;

    public BusStop(String stopServiceid, String stopKname, String stopX, String stopY) {
        this.stopServiceid = stopServiceid;
        this.stopKname = stopKname;
        this.stopX = stopX;
        this.stopY = stopY;
    }

    public static BusStop fromMap(HashMap<String, String> map) {
        return new BusStop(
                value(map, "stopServiceid"),
                value(map, "stopKname"),
                value(map, "stopX"),
                value(map, "stopY")
        );
    }

    public String getStopServiceid() {
        return stopServiceid;
    }

    public String getStopKname() {
        return stopKname;
    }

    public String getStopX() {
        return stopX;
    }

    public String getStopY() {
        return stopY;
    }

    private static String value(HashMap<String, String> map, String key) {
        return map.getOrDefault(key, "");
    }
}
