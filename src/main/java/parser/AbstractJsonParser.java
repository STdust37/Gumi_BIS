package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * JSON 파서의 빈 입력 처리 공통 구현
 */
public abstract class AbstractJsonParser implements JsonParser {
    @Override
    public final ArrayList<HashMap<String, String>> parse(String json) {
        // 비어 있는 JSON 입력에 대한 기본 처리
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }

        // 실제 JSON 구조 파싱은 자식 클래스에서 구현
        return parseNonEmpty(json);
    }

    protected abstract ArrayList<HashMap<String, String>> parseNonEmpty(String json);
}
