package parser;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 파싱된 객체 목록 결과 모델
 */
public class ParsedObjects {
    private final String arrayName;
    private final ArrayList<HashMap<String, String>> objects;

    public ParsedObjects(String arrayName, ArrayList<HashMap<String, String>> objects) {
        // 어떤 JSON 배열을 읽었는지 표시하는 이름 저장
        this.arrayName = arrayName;

        // 외부 목록 변경을 막기 위한 복사 저장
        this.objects = new ArrayList<>(objects);
    }

    public String getArrayName() {
        // 파싱 대상 배열 이름 반환
        return arrayName;
    }

    public ArrayList<HashMap<String, String>> getObjects() {
        // 내부 목록 보호를 위한 복사본 반환
        return new ArrayList<>(objects);
    }
}
