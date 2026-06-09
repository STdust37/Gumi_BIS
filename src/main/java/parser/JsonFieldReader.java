package parser;

import java.util.HashMap;

/**
 * JSON row 필드 값 조회 보조 역할
 */
public class JsonFieldReader {
    public String first(HashMap<String, String> row, String... keys) {
        // row가 없을 때 빈 문자열 반환
        if (row == null) {
            return "";
        }

        // 후보 키를 순서대로 확인
        for (String key : keys) {
            String value = row.getOrDefault(key, "");

            // 처음 발견한 빈 값이 아닌 필드 반환
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        // 사용할 수 있는 값이 없을 때 기본값 반환
        return "";
    }
}
