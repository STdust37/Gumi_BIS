package service;

import model.HotPlaceTransitOption;

import java.util.ArrayList;

/**
 * 핫플 교통편 후보 우선순위 정렬 구현
 */
public class TransitOptionRanker {
    public ArrayList<HotPlaceTransitOption> rank(ArrayList<HotPlaceTransitOption> options) {
        // 원본 추천 목록 보호를 위한 복사본 생성
        ArrayList<HotPlaceTransitOption> sorted = new ArrayList<>(options);

        // 추천 점수와 버스 번호를 기준으로 정렬
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                HotPlaceTransitOption left = sorted.get(i);
                HotPlaceTransitOption right = sorted.get(j);
                if (compare(left, right) > 0) {
                    sorted.set(i, right);
                    sorted.set(j, left);
                }
            }
        }
        return sorted;
    }

    private int compare(HotPlaceTransitOption left, HotPlaceTransitOption right) {
        // 1순위 기준은 추천 점수
        int scoreCompare = Double.compare(left.getScore(), right.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 점수가 같을 때는 버스 번호 기준 정렬
        return left.getArrivalInfo().getBrtId().compareTo(right.getArrivalInfo().getBrtId());
    }
}
