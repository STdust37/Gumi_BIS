package service;

import java.util.ArrayList;

/**
 * 검색 결과 묶음 표현 모델
 */
public class SearchResultGroup<T> {
    private final ArrayList<T> items;

    public SearchResultGroup(ArrayList<T> items) {
        this.items = new ArrayList<>(items);
    }

    public ArrayList<T> getItems() {
        return new ArrayList<>(items);
    }

    public int size() {
        return items.size();
    }
}
