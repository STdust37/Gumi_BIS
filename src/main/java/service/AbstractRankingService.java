package service;

import java.util.ArrayList;

/**
 * 순위 산정 서비스의 공통 정렬 흐름 구현
 */
public abstract class AbstractRankingService<T> {
    public ArrayList<T> top(ArrayList<T> items, int limit) {
        // 원본 목록 보호를 위한 복사본 생성
        ArrayList<T> copy = new ArrayList<>(items);

        // 자식 클래스가 정한 비교 기준으로 정렬
        for (int i = 0; i < copy.size() - 1; i++) {
            for (int j = i + 1; j < copy.size(); j++) {
                T left = copy.get(i);
                T right = copy.get(j);
                if (compare(left, right) > 0) {
                    copy.set(i, right);
                    copy.set(j, left);
                }
            }
        }

        ArrayList<T> result = new ArrayList<>();

        // 정렬된 목록에서 필요한 개수만 복사
        for (T item : copy) {
            result.add(item);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    protected abstract int compare(T left, T right);
}
