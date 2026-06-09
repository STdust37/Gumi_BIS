package service;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 즐겨찾기 저장소 계약 역할
 */
public interface FavoriteRepository {
    void add(String type, String id, String label);

    boolean toggle(String type, String id, String label);

    boolean contains(String type, String id);

    void remove(String type, String id);

    HashMap<String, ArrayList<String>> snapshot();
}
