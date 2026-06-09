package service;

import model.HotPlace;

/**
 * 핫플레이스 점수 기반 순위 산정 구현
 */
public class HotPlaceRankingService extends AbstractRankingService<HotPlace> {
    @Override
    protected int compare(HotPlace left, HotPlace right) {
        // 점수가 높은 핫플이 앞에 오도록 비교
        return Double.compare(right.getScore(), left.getScore());
    }
}
