package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * JSON 문자열 파싱 계약 역할
 */
public interface JsonParser {
    ArrayList<HashMap<String, String>> parse(String json);
}
