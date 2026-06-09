package service;

import model.ArrivalInfo;
import model.BusStop;
import model.HotPlace;
import model.HotPlaceTransitOption;
import model.HotPlaceTransitPlan;
import model.NearbyStopCandidate;
import model.RouteStop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 핫플레이스 주변 정류장 기반 교통편 추천 구현
 */
public class HotPlaceTransitRecommender implements TransitRecommender {
    private static final int DESTINATION_CANDIDATE_COUNT = 5;

    private final BisSearchService service;
    private final DestinationStopResolver destinationStopResolver = new DestinationStopResolver();

    public HotPlaceTransitRecommender(BisSearchService service) {
        this.service = service;
    }

    public NearbyStopCandidate resolvePrimaryDestination(HotPlace hotPlace) {
        // 핫플에서 가장 가까운 대표 도착 정류장 탐색
        return destinationStopResolver.resolvePrimary(hotPlace, service.getStops());
    }

    public HotPlaceTransitPlan recommend(HotPlace hotPlace, BusStop departureStop) throws IOException, InterruptedException {
        // 핫플 주변 정류장을 가까운 순서로 여러 개 확보
        ArrayList<NearbyStopCandidate> destinationCandidates =
                destinationStopResolver.resolveTop(hotPlace, service.getStops(), DESTINATION_CANDIDATE_COUNT);

        // 화면에 안내할 대표 도착 정류장 선택
        NearbyStopCandidate primaryDestination = destinationCandidates.isEmpty() ? null : destinationCandidates.get(0);
        ArrayList<HotPlaceTransitOption> options = new ArrayList<>();
        if (primaryDestination == null) {
            return new HotPlaceTransitPlan(hotPlace, departureStop, null, options);
        }

        // 정류장 ID로 도착 후보를 바로 찾기 위한 임시 표
        HashMap<String, NearbyStopCandidate> candidateByStopId = new HashMap<>();
        for (NearbyStopCandidate candidate : destinationCandidates) {
            candidateByStopId.put(candidate.getStop().getStopServiceid(), candidate);
        }

        // 같은 노선이 여러 번 잡힐 수 있으므로 노선별 최적 결과만 보관
        HashMap<String, HotPlaceTransitOption> bestByRoute = new HashMap<>();

        // 출발 정류장의 현재 도착 정보 목록 수집
        ArrayList<ArrivalInfo> arrivals = service.getArrivalInfo(departureStop.getStopServiceid());
        for (ArrivalInfo arrival : arrivals) {
            // 해당 버스 노선의 전체 정류장 순서 확인
            ArrayList<RouteStop> routeStops = service.getRouteStops(arrival.getRouteId());

            // 노선도 안에서 출발 정류장 위치 확인
            int departureIndex = findStopIndex(routeStops, departureStop);
            if (departureIndex < 0) {
                continue;
            }

            // 출발 정류장 이후에 핫플 주변 정류장이 나오는지 확인
            DestinationMatch match = findDestinationAfterDeparture(routeStops, departureIndex, candidateByStopId);
            if (match == null) {
                continue;
            }

            // 이동 정류장 수와 대기 시간을 이용한 추천 점수 계산
            int stopsBetween = Math.max(0, match.stopIndex - departureIndex);
            double score = score(match.candidate, arrival, stopsBetween);
            HotPlaceTransitOption option = new HotPlaceTransitOption(
                    hotPlace, departureStop, match.candidate, arrival, stopsBetween, score);

            // 같은 routeId에서는 점수가 더 낮은 결과를 우선 선택
            HotPlaceTransitOption previous = bestByRoute.get(arrival.getRouteId());
            if (previous == null || option.getScore() < previous.getScore()) {
                bestByRoute.put(arrival.getRouteId(), option);
            }
        }

        options.addAll(bestByRoute.values());
        sortOptions(options);

        return new HotPlaceTransitPlan(hotPlace, departureStop, primaryDestination, options);
    }

    private int findStopIndex(ArrayList<RouteStop> routeStops, BusStop stop) {
        // 정류장 ID와 이름을 모두 비교하기 위한 기준값 준비
        String stopId = normalize(stop.getStopServiceid());
        String stopName = normalize(stop.getStopKname());
        for (int i = 0; i < routeStops.size(); i++) {
            RouteStop routeStop = routeStops.get(i);
            // ID가 일치하거나 이름이 일치하면 같은 정류장으로 판단
            if (normalize(routeStop.getServiceId()).equals(stopId)
                    || normalize(routeStop.getStopName()).equals(stopName)) {
                return i;
            }
        }
        return -1;
    }

    private DestinationMatch findDestinationAfterDeparture(ArrayList<RouteStop> routeStops, int departureIndex,
                                                          HashMap<String, NearbyStopCandidate> candidateByStopId) {
        DestinationMatch best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        // 출발 정류장 다음 위치부터 도착 후보 정류장 검색
        for (int i = departureIndex + 1; i < routeStops.size(); i++) {
            RouteStop routeStop = routeStops.get(i);
            NearbyStopCandidate candidate = candidateByStopId.get(routeStop.getServiceId());
            if (candidate == null) {
                continue;
            }

            // 거리와 이동 정류장 수를 함께 반영한 후보 점수
            double matchScore = candidate.getDistanceMeters() + ((i - departureIndex) * 15.0);
            if (matchScore < bestScore) {
                bestScore = matchScore;
                best = new DestinationMatch(candidate, i);
            }
        }
        return best;
    }

    private double score(NearbyStopCandidate destinationCandidate, ArrivalInfo arrival, int stopsBetween) {
        // 도착 정류장까지 걷는 거리 점수
        double distanceScore = destinationCandidate.getDistanceMeters();

        // 버스 대기 시간이 없으면 큰 벌점 부여
        int waitSeconds = waitSeconds(arrival);
        double waitScore = waitSeconds >= 0 ? (waitSeconds / 60.0) * 20.0 : 800.0;

        // 버스를 타고 지나가는 정류장 수 점수
        double stopCountScore = stopsBetween * 5.0;

        return distanceScore + waitScore + stopCountScore;
    }

    private int waitSeconds(ArrivalInfo arrival) {
        // 초 단위 값이 있으면 우선 사용
        int seconds = parseInt(arrival.getRemainTimeSec());
        if (seconds >= 0) {
            return seconds;
        }

        // 초 단위 값이 없으면 분 단위 값을 초로 변환
        int minutes = parseInt(arrival.getRemainTime());
        return minutes >= 0 ? minutes * 60 : -1;
    }

    private int parseInt(String value) {
        // 숫자로 바꿀 수 없는 값은 -1로 처리
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String normalize(String value) {
        // null 비교 오류를 피하기 위한 빈 문자열 변환
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private void sortOptions(ArrayList<HotPlaceTransitOption> options) {
        // 추천 점수 오름차순 정렬 구현
        for (int i = 0; i < options.size() - 1; i++) {
            for (int j = i + 1; j < options.size(); j++) {
                HotPlaceTransitOption left = options.get(i);
                HotPlaceTransitOption right = options.get(j);
                if (compareOption(left, right) > 0) {
                    options.set(i, right);
                    options.set(j, left);
                }
            }
        }
    }

    private int compareOption(HotPlaceTransitOption left, HotPlaceTransitOption right) {
        // 1순위 기준은 추천 점수
        int scoreCompare = Double.compare(left.getScore(), right.getScore());
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 점수가 같을 때는 버스 번호 기준 정렬
        return left.getArrivalInfo().getBrtId().compareTo(right.getArrivalInfo().getBrtId());
    }

    private static final class DestinationMatch {
        private final NearbyStopCandidate candidate;
        private final int stopIndex;

        private DestinationMatch(NearbyStopCandidate candidate, int stopIndex) {
            this.candidate = candidate;
            this.stopIndex = stopIndex;
        }
    }
}
