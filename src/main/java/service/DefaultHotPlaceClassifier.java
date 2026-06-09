package service;

import model.HotPlace;

/**
 * 핫플레이스 카테고리 분류 기본 구현
 */
public class DefaultHotPlaceClassifier implements HotPlaceClassifier {
    @Override
    public boolean isAttraction(HotPlace hotPlace) {
        return HotPlaceService.isAttractionLike(hotPlace);
    }

    @Override
    public boolean isFood(HotPlace hotPlace) {
        return HotPlaceService.isFoodLike(hotPlace);
    }
}
