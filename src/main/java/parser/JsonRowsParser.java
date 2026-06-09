package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * BIS rows 배열 JSON 파싱 구현
 */
public final class JsonRowsParser {
    private JsonRowsParser() {
    }

    public static ArrayList<HashMap<String, String>> parseRows(String json) {
        ArrayList<HashMap<String, String>> rows = new ArrayList<>();

        // 응답이 비어 있으면 빈 목록 반환
        if (json == null || json.isBlank()) {
            return rows;
        }

        // BIS 응답에서 "rows"라는 배열 위치 찾기
        int rowsKey = findString(json, "rows", 0);
        if (rowsKey < 0) {
            return rows;
        }

        // "rows": [ ... ] 구조에서 배열 시작 위치 찾기
        int colon = json.indexOf(':', rowsKey);
        int arrayStart = json.indexOf('[', colon);
        if (colon < 0 || arrayStart < 0) {
            return rows;
        }

        // 배열의 끝 위치 찾기
        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            return rows;
        }

        // 배열 안에 있는 JSON 객체를 하나씩 HashMap으로 변환
        int index = arrayStart + 1;
        while (index < arrayEnd) {
            char c = json.charAt(index);
            if (c == '{') {
                // 현재 객체의 닫는 중괄호 위치 찾기
                int objectEnd = findMatching(json, index, '{', '}');
                if (objectEnd < 0 || objectEnd > arrayEnd) {
                    break;
                }

                // 객체 내부의 key/value를 HashMap으로 변환
                rows.add(parseObject(json, index + 1, objectEnd));
                index = objectEnd + 1;
            } else {
                index++;
            }
        }
        return rows;
    }

    private static HashMap<String, String> parseObject(String json, int start, int end) {
        HashMap<String, String> map = new HashMap<>();
        int index = start;

        // 객체 안의 "key": value 구조를 반복해서 읽는 처리
        while (index < end) {
            index = skipWhitespaceAndCommas(json, index, end);
            if (index >= end || json.charAt(index) != '"') {
                index++;
                continue;
            }

            // 따옴표로 감싼 key 읽기
            ParseResult key = parseString(json, index);
            index = skipWhitespace(json, key.nextIndex, end);
            if (index >= end || json.charAt(index) != ':') {
                break;
            }

            // 콜론 뒤에 있는 value 읽기
            index = skipWhitespace(json, index + 1, end);
            ParseResult value = parseValue(json, index, end);
            map.put(key.value, value.value);
            index = value.nextIndex;
        }
        return map;
    }

    private static ParseResult parseValue(String json, int index, int end) {
        if (index >= end) {
            return new ParseResult("", index);
        }

        char c = json.charAt(index);

        // 문자열 값 처리
        if (c == '"') {
            return parseString(json, index);
        }

        // null 값은 빈 문자열로 처리
        if (startsWith(json, index, "null")) {
            return new ParseResult("", index + 4);
        }

        // 숫자나 boolean 같은 따옴표 없는 값 처리
        int valueEnd = index;
        while (valueEnd < end) {
            char current = json.charAt(valueEnd);
            if (current == ',' || current == '}') {
                break;
            }
            valueEnd++;
        }
        return new ParseResult(json.substring(index, valueEnd).trim(), valueEnd);
    }

    private static ParseResult parseString(String json, int quoteIndex) {
        StringBuilder builder = new StringBuilder();
        int index = quoteIndex + 1;

        // 시작 따옴표 다음부터 끝 따옴표까지 문자 읽기
        while (index < json.length()) {
            char c = json.charAt(index);
            if (c == '"') {
                return new ParseResult(builder.toString(), index + 1);
            }

            // 역슬래시로 시작하는 escape 문자 처리
            if (c == '\\' && index + 1 < json.length()) {
                char escaped = json.charAt(index + 1);
                if (escaped == 'u' && index + 5 < json.length()) {
                    String hex = json.substring(index + 2, index + 6);
                    try {
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 6;
                        continue;
                    } catch (NumberFormatException ignored) {
                        builder.append("\\u").append(hex);
                        index += 6;
                        continue;
                    }
                }
                builder.append(unescape(escaped));
                index += 2;
                continue;
            }
            builder.append(c);
            index++;
        }
        return new ParseResult(builder.toString(), index);
    }

    private static char unescape(char escaped) {
        if (escaped == '"') return '"';
        if (escaped == '\\') return '\\';
        if (escaped == '/') return '/';
        if (escaped == 'b') return '\b';
        if (escaped == 'f') return '\f';
        if (escaped == 'n') return '\n';
        if (escaped == 'r') return '\r';
        if (escaped == 't') return '\t';
        return escaped;
    }

    private static int findString(String json, String value, int start) {
        String quoted = "\"" + value + "\"";
        return json.indexOf(quoted, start);
    }

    private static int findMatching(String json, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        // 괄호 깊이를 계산해서 짝이 맞는 닫는 괄호 위치 찾기
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int skipWhitespaceAndCommas(String json, int index, int end) {
        while (index < end) {
            char c = json.charAt(index);
            if (!Character.isWhitespace(c) && c != ',') {
                return index;
            }
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String json, int index, int end) {
        while (index < end && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean startsWith(String value, int offset, String target) {
        return offset + target.length() <= value.length() && value.startsWith(target, offset);
    }

    private static final class ParseResult {
        private final String value;
        private final int nextIndex;

        private ParseResult(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
