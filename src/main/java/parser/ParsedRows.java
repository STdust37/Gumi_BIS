package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 파싱된 rows 목록 결과 모델
 */
public class ParsedRows {
    private final ArrayList<HashMap<String, String>> rows;

    public ParsedRows(ArrayList<HashMap<String, String>> rows) {
        // 외부 목록 변경을 막기 위한 복사 저장
        this.rows = new ArrayList<>(rows);
    }

    public ArrayList<HashMap<String, String>> getRows() {
        // 내부 목록 보호를 위한 복사본 반환
        return new ArrayList<>(rows);
    }

    public int size() {
        // 파싱된 row 개수 반환
        return rows.size();
    }
}
