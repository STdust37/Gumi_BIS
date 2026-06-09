package service;

import model.HotPlace;

/**
 * 핫플레이스 유형 판별 계약 역할
 */
public interface HotPlaceClassifier {
    boolean isAttraction(HotPlace hotPlace);

    boolean isFood(HotPlace hotPlace);
}
