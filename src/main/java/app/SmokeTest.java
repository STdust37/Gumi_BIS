package app;

import model.Route;
import parser.JsonRowsParser;

import java.util.ArrayList;
import java.util.HashMap;

public class SmokeTest {
    public static void main(String[] args) {
        String json = "{\"rows\":[{\"routeId\":\"1010\",\"brtId\":\"10\",\"remark\":\"구미역_인동\",\"cnt\":1},{\"routeId\":\"수점20\",\"brtId\":\"수점\",\"remark\":null}]}";
        ArrayList<HashMap<String, String>> rows = JsonRowsParser.parseRows(json);
        assertEquals("row count", "2", String.valueOf(rows.size()));
        assertEquals("routeId", "1010", rows.get(0).get("routeId"));
        assertEquals("korean value", "구미역_인동", rows.get(0).get("remark"));
        assertEquals("null value", "", rows.get(1).get("remark"));

        Route route = Route.fromMap(rows.get(0));
        assertEquals("model brtId", "10", route.getBrtId());

        System.out.println("SmokeTest OK");
    }

    private static void assertEquals(String name, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(name + " expected '" + expected + "' but got '" + actual + "'");
        }
    }
}
