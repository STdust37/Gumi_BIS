package service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 메모리 기반 즐겨찾기 관리 구현
 */
public class FavoriteStore implements FavoriteRepository {
    private final HashMap<String, ArrayList<String>> favorites = new HashMap<>();

    public FavoriteStore() {
        favorites.put("STOP", new ArrayList<>());
        favorites.put("ROUTE", new ArrayList<>());
    }

    public void add(String type, String id, String label) {
        // 저장 형식: "ID | 화면에 보여줄 이름" 문자열 생성
        String entry = id + " | " + label;

        // 즐겨찾기 종류(STOP/ROUTE)에 해당하는 목록 조회
        ArrayList<String> entries = favorites.get(type);
        if (entries == null) {
            entries = new ArrayList<>();
            favorites.put(type, entries);
        }

        // 같은 항목이 이미 있으면 중복 저장 방지
        if (!entries.contains(entry)) {
            entries.add(entry);
        }
    }

    public boolean toggle(String type, String id, String label) {
        // 이미 등록된 항목이면 제거 처리
        if (contains(type, id)) {
            remove(type, id);
            return false;
        }

        // 등록되지 않은 항목이면 새로 추가 처리
        add(type, id, label);
        return true;
    }

    public boolean contains(String type, String id) {
        // 즐겨찾기 종류에 맞는 목록 조회
        ArrayList<String> entries = favorites.get(type);
        if (entries == null) {
            return false;
        }

        // 저장 문자열의 앞부분 ID 비교
        String prefix = id + " | ";
        for (String entry : entries) {
            if (entry.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public void remove(String type, String id) {
        // 삭제할 즐겨찾기 목록 조회
        ArrayList<String> entries = favorites.get(type);
        if (entries == null) {
            return;
        }

        // removeIf 대신 직접 반복하면서 삭제할 위치 탐색
        String prefix = id + " | ";
        for (int i = entries.size() - 1; i >= 0; i--) {
            String entry = entries.get(i);
            if (entry.startsWith(prefix)) {
                entries.remove(i);
            }
        }
    }

    public HashMap<String, ArrayList<String>> snapshot() {
        // 외부에서 원본 목록을 직접 수정하지 못하도록 복사본 생성
        HashMap<String, ArrayList<String>> copy = new HashMap<>();
        for (String key : favorites.keySet()) {
            copy.put(key, new ArrayList<>(favorites.get(key)));
        }
        return copy;
    }
}
