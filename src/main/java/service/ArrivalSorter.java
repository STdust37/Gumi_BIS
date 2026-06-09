package service;

import model.ArrivalInfo;

import java.util.ArrayList;

/**
 * 도착 정보 번호순과 도착순 정렬 구현
 */
public class ArrivalSorter {
    public ArrayList<ArrivalInfo> byRouteNumber(ArrayList<ArrivalInfo> arrivals) {
        // 원본 목록 보호를 위한 복사본 생성
        ArrayList<ArrivalInfo> sorted = new ArrayList<>(arrivals);

        // 버스 번호 기준 오름차순 정렬
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                ArrivalInfo left = sorted.get(i);
                ArrivalInfo right = sorted.get(j);
                if (left.getBrtId().compareTo(right.getBrtId()) > 0) {
                    sorted.set(i, right);
                    sorted.set(j, left);
                }
            }
        }
        return sorted;
    }

    public ArrayList<ArrivalInfo> byArrivalTime(ArrayList<ArrivalInfo> arrivals) {
        // 원본 목록 보호를 위한 복사본 생성
        ArrayList<ArrivalInfo> sorted = new ArrayList<>(arrivals);

        // 남은 시간이 짧은 순서로 정렬
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                ArrivalInfo left = sorted.get(i);
                ArrivalInfo right = sorted.get(j);
                if (seconds(left) > seconds(right)) {
                    sorted.set(i, right);
                    sorted.set(j, left);
                }
            }
        }
        return sorted;
    }

    private int seconds(ArrivalInfo arrival) {
        // 숫자로 바꿀 수 없는 도착 시간은 가장 뒤쪽 배치
        try {
            return Integer.parseInt(arrival.getRemainTimeSec());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
