package service;

import model.HotPlaceTransitOption;

import java.util.ArrayList;

/**
 * 핫플 교통편 추천 결과값 모델
 */
public class TransitSearchResult {
    private final ArrayList<HotPlaceTransitOption> options;

    public TransitSearchResult(ArrayList<HotPlaceTransitOption> options) {
        this.options = new ArrayList<>(options);
    }

    public ArrayList<HotPlaceTransitOption> getOptions() {
        return new ArrayList<>(options);
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }
}
