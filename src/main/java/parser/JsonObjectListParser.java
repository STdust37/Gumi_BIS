package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 지정 배열 키 기반 JSON 객체 목록 파싱 구현
 */
public final class JsonObjectListParser {
    private JsonObjectListParser() {
    }

    public static ArrayList<HashMap<String, String>> parseNestedList(String json, String parentKey, String listKey) {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();

        // 응답이 비어 있으면 빈 목록 반환
        if (json == null || json.isBlank()) {
            return result;
        }

        // 먼저 부모 key 위치 찾기
        int parentIndex = findString(json, parentKey, 0);
        if (parentIndex < 0) {
            return result;
        }

        // 부모 key 이후에 있는 실제 배열 key 위치 찾기
        int listIndex = findString(json, listKey, parentIndex);
        if (listIndex < 0) {
            return result;
        }

        int colon = json.indexOf(':', listIndex);
        int arrayStart = json.indexOf('[', colon);
        if (colon < 0 || arrayStart < 0) {
            return result;
        }

        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            return result;
        }

        int index = arrayStart + 1;
        while (index < arrayEnd) {
            if (json.charAt(index) == '{') {
                int objectEnd = findMatching(json, index, '{', '}');
                if (objectEnd < 0 || objectEnd > arrayEnd) {
                    break;
                }
                result.add(parseObject(json, index + 1, objectEnd));
                index = objectEnd + 1;
            } else {
                index++;
            }
        }
        return result;
    }

    public static ArrayList<HashMap<String, String>> parseList(String json, String listKey) {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();

        // 응답이 비어 있으면 빈 목록 반환
        if (json == null || json.isBlank()) {
            return result;
        }

        // 지정한 배열 key 위치 찾기
        int listIndex = findString(json, listKey, 0);
        if (listIndex < 0) {
            return result;
        }

        // 배열의 시작과 끝 위치 계산
        int colon = json.indexOf(':', listIndex);
        int arrayStart = json.indexOf('[', colon);
        if (colon < 0 || arrayStart < 0) {
            return result;
        }

        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        if (arrayEnd < 0) {
            return result;
        }

        int index = arrayStart + 1;
        while (index < arrayEnd) {
            if (json.charAt(index) == '{') {
                // 배열 내부 객체 하나의 끝 위치 찾기
                int objectEnd = findMatching(json, index, '{', '}');
                if (objectEnd < 0 || objectEnd > arrayEnd) {
                    break;
                }

                // 객체를 HashMap으로 변환해서 결과 목록에 추가
                result.add(parseObject(json, index + 1, objectEnd));
                index = objectEnd + 1;
            } else {
                index++;
            }
        }
        return result;
    }

    private static HashMap<String, String> parseObject(String json, int start, int end) {
        HashMap<String, String> map = new HashMap<>();
        int index = start;

        // 객체 내부의 key/value를 순서대로 읽는 처리
        while (index < end) {
            index = skipWhitespaceAndCommas(json, index, end);
            if (index >= end || json.charAt(index) != '"') {
                index++;
                continue;
            }

            ParseResult key = parseString(json, index);
            index = skipWhitespace(json, key.nextIndex, end);
            if (index >= end || json.charAt(index) != ':') {
                break;
            }

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

        // 중첩 객체는 현재 프로젝트에서 사용하지 않으므로 건너뛰는 처리
        if (c == '{') {
            int objectEnd = findMatching(json, index, '{', '}');
            return new ParseResult("", objectEnd < 0 ? end : objectEnd + 1);
        }

        // 중첩 배열은 현재 프로젝트에서 사용하지 않으므로 건너뛰는 처리
        if (c == '[') {
            int arrayEnd = findMatching(json, index, '[', ']');
            return new ParseResult("", arrayEnd < 0 ? end : arrayEnd + 1);
        }

        // null 값은 빈 문자열로 처리
        if (startsWith(json, index, "null")) {
            return new ParseResult("", index + 4);
        }

        // 따옴표 없는 숫자/boolean 값 처리
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
        // 따옴표 안의 실제 문자열을 누적할 공간
        StringBuilder builder = new StringBuilder();

        // 시작 따옴표 다음 문자부터 읽기 시작
        int index = quoteIndex + 1;
        while (index < json.length()) {
            // 현재 읽고 있는 문자
            char c = json.charAt(index);

            // 닫는 따옴표를 만나면 문자열 파싱 종료
            if (c == '"') {
                return new ParseResult(builder.toString(), index + 1);
            }

            // 역슬래시로 시작하는 escape 문자 처리
            if (c == '\\' && index + 1 < json.length()) {
                char escaped = json.charAt(index + 1);

                // \uAC00 같은 유니코드 escape 처리
                if (escaped == 'u' && index + 5 < json.length()) {
                    String hex = json.substring(index + 2, index + 6);
                    try {
                        // 16진수 값을 실제 문자로 변환
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 6;
                        continue;
                    } catch (NumberFormatException ignored) {
                        // 잘못된 유니코드 escape는 원문 형태로 보존
                        builder.append("\\u").append(hex);
                        index += 6;
                        continue;
                    }
                }

                // 일반 escape 문자를 실제 문자로 변환
                builder.append(unescape(escaped));
                index += 2;
                continue;
            }

            // 일반 문자는 그대로 결과 문자열에 추가
            builder.append(c);
            index++;
        }

        // 닫는 따옴표를 찾지 못한 경우 현재까지 읽은 문자열 반환
        return new ParseResult(builder.toString(), index);
    }

    private static char unescape(char escaped) {
        // JSON escape 문자별 실제 문자 변환
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
        // JSON key 형태인 "value" 문자열 위치 탐색
        return json.indexOf("\"" + value + "\"", start);
    }

    private static int findMatching(String json, int start, char open, char close) {
        // 중첩된 괄호 깊이 계산값
        int depth = 0;

        // 현재 위치가 문자열 내부인지 여부
        boolean inString = false;

        // 문자열 내부에서 직전 문자가 escape였는지 여부
        boolean escaped = false;

        // 시작 위치부터 끝까지 이동하며 짝이 맞는 닫는 괄호 탐색
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                // escape 다음 문자는 구조 문자로 보지 않는 처리
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    // 다음 문자가 escape 대상임을 표시
                    escaped = true;
                } else if (c == '"') {
                    // 문자열 영역 종료
                    inString = false;
                }
                continue;
            }

            // 문자열 영역 시작
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                // 여는 괄호를 만나면 깊이 증가
                depth++;
            } else if (c == close) {
                // 닫는 괄호를 만나면 깊이 감소
                depth--;
                if (depth == 0) {
                    // 시작 괄호와 짝이 맞는 닫는 괄호 위치 반환
                    return i;
                }
            }
        }

        // 짝이 맞는 괄호를 찾지 못한 경우
        return -1;
    }

    private static int skipWhitespaceAndCommas(String json, int index, int end) {
        // 공백과 쉼표를 건너뛰며 다음 의미 있는 문자 위치 탐색
        while (index < end) {
            char c = json.charAt(index);
            if (!Character.isWhitespace(c) && c != ',') {
                // 공백이나 쉼표가 아닌 문자의 위치 반환
                return index;
            }
            index++;
        }
        // 범위 끝까지 이동한 위치 반환
        return index;
    }

    private static int skipWhitespace(String json, int index, int end) {
        // 공백 문자를 건너뛰는 처리
        while (index < end && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        // 공백이 끝난 위치 반환
        return index;
    }

    private static boolean startsWith(String value, int offset, String target) {
        // offset 위치부터 target 문자열이 시작되는지 확인
        return offset + target.length() <= value.length() && value.startsWith(target, offset);
    }

    private static final class ParseResult {
        // 파싱된 문자열 값
        private final String value;

        // 다음에 읽어야 할 문자열 위치
        private final int nextIndex;

        private ParseResult(String value, int nextIndex) {
            // 파싱 결과 값 저장
            this.value = value;

            // 다음 시작 위치 저장
            this.nextIndex = nextIndex;
        }
    }
}
