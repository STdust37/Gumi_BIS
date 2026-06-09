package service;

import model.HotPlace;

/**
 * 핫플레이스 점수 계산 전략 역할
 */
public interface HotPlaceScorer {
    HotPlace score(HotPlace hotPlace, HotPlaceScoreContext context);
}
