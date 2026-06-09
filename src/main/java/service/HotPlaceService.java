package service;

import client.GooglePlacesClient;
import client.NaverSearchHotPlaceClient;
import model.HotPlace;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 핫플레이스 수집과 점수 보강 흐름 구현
 */
public class HotPlaceService {
    private static final int TOP_LIMIT = 10;

    private final NaverSearchHotPlaceClient naverClient;
    private final GooglePlacesClient googlePlacesClient;
    private String lastLoadMessage = "REST 조회 대기";
    private String lastErrorMessage = "";
    private boolean googleLookupFailed;

    public HotPlaceService(NaverSearchHotPlaceClient naverClient, GooglePlacesClient googlePlacesClient) {
        this.naverClient = naverClient;
        this.googlePlacesClient = googlePlacesClient;
    }

    public HotPlaceResult loadHotPlaceResult() throws InterruptedException {
        ArrayList<HotPlace> hotPlaces;
        try {
            // 이전 조회 상태 초기화
            lastErrorMessage = "";
            googleLookupFailed = false;

            // 네이버 후보 목록에 외부 지표 보강
            hotPlaces = enrichHotPlaces(naverClient.searchHotPlaceCandidates());

            // 보강된 지표 기반 핫플 점수 계산
            scoreHotPlaces(hotPlaces);
        } catch (IOException e) {
            // 외부 조회 실패 시 기본 후보 목록 사용
            lastErrorMessage = e.getMessage() == null ? "" : e.getMessage();
            hotPlaces = fallbackHotPlaces();
            return buildResultWithErrorMessage(hotPlaces, e);
        }
        sortByScoreDescending(hotPlaces);

        // 전체, 명소, 먹거리 목록 분리
        HotPlaceResult result = splitResult(hotPlaces);
        if (googleLookupFailed) {
            lastLoadMessage = "Google 조회 실패: " + result.countSummary();
        } else {
            lastLoadMessage = googlePlacesClient.hasApiKey()
                    ? "네이버+Google: " + result.countSummary()
                    : "Google 키 없음: " + result.countSummary();
        }
        return result;
    }

    public ArrayList<HotPlace> loadTopHotPlaces() throws InterruptedException {
        return loadHotPlaceResult().getAllPlaces();
    }

    public String getLastLoadMessage() {
        return lastLoadMessage;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private ArrayList<HotPlace> enrichHotPlaces(ArrayList<HotPlace> candidates) throws IOException, InterruptedException {
        ArrayList<HotPlace> enriched = new ArrayList<>();

        // 후보 장소마다 언급량, 리뷰 수, 평점 조회
        for (HotPlace candidate : candidates) {
            int mentionCount = countMentions(candidate);
            GooglePlacesClient.PlaceStats placeStats = findGoogleStats(candidate);
            enriched.add(candidate.withMetrics(mentionCount, placeStats.getReviewCount(), placeStats.getRating()));
        }
        return enriched;
    }

    private int countMentions(HotPlace candidate) throws InterruptedException {
        try {
            // 네이버 블로그 검색 결과 수 조회
            return naverClient.countRecentBlogMentions(candidate.getName());
        } catch (IOException e) {
            // 언급량 조회 실패 시 0건 처리
            return 0;
        }
    }

    private GooglePlacesClient.PlaceStats findGoogleStats(HotPlace candidate) throws InterruptedException {
        try {
            // 구글 장소 정보에서 리뷰 수와 평점 조회
            return googlePlacesClient.findPlaceStats(candidate.getName());
        } catch (IOException e) {
            // 구글 조회 실패 여부 기록
            googleLookupFailed = googlePlacesClient.hasApiKey();
            return GooglePlacesClient.PlaceStats.empty();
        }
    }

    private void scoreHotPlaces(ArrayList<HotPlace> hotPlaces) {
        // 정규화 기준으로 사용할 최대값 계산
        int maxMentionCount = 0;
        int maxReviewCount = 0;
        for (HotPlace hotPlace : hotPlaces) {
            maxMentionCount = Math.max(maxMentionCount, hotPlace.getMentionCount());
            maxReviewCount = Math.max(maxReviewCount, hotPlace.getReviewCount());
        }

        for (int i = 0; i < hotPlaces.size(); i++) {
            HotPlace hotPlace = hotPlaces.get(i);

            // 서로 단위가 다른 지표를 0~100 범위로 변환
            double mentionScore = logNormalize(hotPlace.getMentionCount(), maxMentionCount);
            double reviewScore = logNormalize(hotPlace.getReviewCount(), maxReviewCount);
            double ratingScore = hotPlace.getRating() <= 0.0 ? 0.0 : Math.min(100.0, hotPlace.getRating() / 5.0 * 100.0);
            double categoryScore = categoryScore(hotPlace);

            // 구글 데이터 사용 가능 여부에 따른 가중치 선택
            double score;
            if (googlePlacesClient.hasApiKey() && !googleLookupFailed) {
                score = (mentionScore * 0.4) + (reviewScore * 0.25) + (ratingScore * 0.2) + (categoryScore * 0.15);
            } else {
                score = (mentionScore * 0.65) + (categoryScore * 0.35);
            }
            hotPlaces.set(i, hotPlace.withScore(score));
        }
    }

    private double logNormalize(int value, int maxValue) {
        // 0 이하 값은 점수 없음 처리
        if (value <= 0 || maxValue <= 0) {
            return 0.0;
        }

        // 큰 값이 점수를 과도하게 지배하지 않도록 로그 정규화
        return Math.log(value + 1.0) / Math.log(maxValue + 1.0) * 100.0;
    }

    private HotPlaceResult buildResultWithErrorMessage(ArrayList<HotPlace> hotPlaces, IOException e) {
        sortByScoreDescending(hotPlaces);
        HotPlaceResult result = splitResult(hotPlaces);
        if (e.getMessage() != null && e.getMessage().contains("NAVER_CLIENT")) {
            lastLoadMessage = "네이버 키 없음: " + result.countSummary();
        } else {
            lastLoadMessage = "네이버 조회 실패: " + compact(lastErrorMessage);
        }
        return result;
    }

    private HotPlaceResult splitResult(ArrayList<HotPlace> sortedPlaces) {
        ArrayList<HotPlace> attractions = new ArrayList<>();
        ArrayList<HotPlace> foods = new ArrayList<>();

        // 장소 성격에 따라 명소와 먹거리 목록 분리
        for (HotPlace hotPlace : sortedPlaces) {
            if (isAttractionLike(hotPlace)) {
                attractions.add(hotPlace);
            } else if (isFoodLike(hotPlace)) {
                foods.add(hotPlace);
            }
        }
        return new HotPlaceResult(
                topPlaces(sortedPlaces, TOP_LIMIT),
                topPlaces(attractions, TOP_LIMIT),
                topPlaces(foods, TOP_LIMIT));
    }

    private ArrayList<HotPlace> topPlaces(ArrayList<HotPlace> places, int limit) {
        ArrayList<HotPlace> topPlaces = new ArrayList<>();

        // 화면에 보여줄 개수만 앞에서부터 복사
        for (HotPlace hotPlace : places) {
            topPlaces.add(hotPlace);
            if (topPlaces.size() >= limit) {
                break;
            }
        }
        return topPlaces;
    }

    public static boolean isAttractionLike(HotPlace hotPlace) {
        // 음식점 계열 키워드는 명소 분류에서 제외
        String categoryText = text(hotPlace.getCategory());
        if (containsAny(categoryText, "카페", "음식", "맛집", "식당", "디저트", "베이커리", "레스토랑")) {
            return false;
        }

        // 장소 이름과 카테고리를 합쳐 명소 키워드 확인
        String combinedText = text(hotPlace.getCategory() + " " + hotPlace.getName());
        return containsAny(combinedText,
                "관광", "명소", "공원", "문화", "전시", "박물관", "미술관", "시장", "테마", "역사", "기념",
                "산", "자연", "수목원", "정원", "호수", "강", "사찰", "절", "전망", "체험", "놀이", "레저",
                "유적", "도립공원", "생태", "광장");
    }

    public static boolean isFoodLike(HotPlace hotPlace) {
        // 장소 이름과 카테고리를 합쳐 먹거리 키워드 확인
        String combinedText = text(hotPlace.getCategory() + " " + hotPlace.getName());
        return containsAny(combinedText,
                "카페", "디저트", "음식", "맛집", "식당", "한식", "중식", "일식", "양식", "분식", "뷔페",
                "고기", "치킨", "피자", "패스트푸드", "베이커리", "술집", "호프", "레스토랑", "커피",
                "떡볶이", "국밥", "갈비");
    }

    private double categoryScore(HotPlace hotPlace) {
        // 명소와 먹거리 성격에 따른 보정 점수 계산
        if (isAttractionLike(hotPlace)) {
            return 100.0;
        }
        if (isFoodLike(hotPlace)) {
            return 55.0;
        }
        return 70.0;
    }

    private static boolean containsAny(String value, String... keywords) {
        // 키워드 배열 중 하나라도 포함되는지 확인
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String text(String value) {
        // null 문자열 비교를 피하기 위한 기본값 처리
        return value == null ? "" : value.toLowerCase();
    }

    private String compact(String message) {
        // 긴 오류 메시지는 화면에 맞게 짧게 변환
        if (message == null || message.isBlank()) {
            return "기본 후보 " + fallbackHotPlaces().size() + "개 표시";
        }
        String compact = message.replaceAll("\\s+", " ").trim();
        return compact.length() <= 70 ? compact : compact.substring(0, 70) + "...";
    }

    private ArrayList<HotPlace> fallbackHotPlaces() {
        // API 실패 시 사용할 기본 핫플 목록
        ArrayList<HotPlace> hotPlaces = new ArrayList<>();
        hotPlaces.add(new HotPlace("FALLBACK_001", "금오산 도립공원", "관광", "경북 구미시 남통동", "128.3137", "36.1011", -1, 0, 0.0, 96));
        hotPlaces.add(new HotPlace("FALLBACK_002", "동락공원", "공원", "경북 구미시 진평동", "128.4019", "36.0986", -1, 0, 0.0, 91));
        hotPlaces.add(new HotPlace("FALLBACK_003", "구미중앙시장", "시장", "경북 구미시 원평동", "128.3375", "36.1307", -1, 0, 0.0, 87));
        hotPlaces.add(new HotPlace("FALLBACK_004", "새마을운동테마공원", "문화", "경북 구미시 상모동", "128.3412", "36.0918", -1, 0, 0.0, 82));
        hotPlaces.add(new HotPlace("FALLBACK_005", "구미코", "전시", "경북 구미시 산동읍", "128.4557", "36.1373", -1, 0, 0.0, 78));
        return hotPlaces;
    }

    private void sortByScoreDescending(ArrayList<HotPlace> hotPlaces) {
        // 핫플 점수 내림차순 정렬 구현
        for (int i = 0; i < hotPlaces.size() - 1; i++) {
            for (int j = i + 1; j < hotPlaces.size(); j++) {
                HotPlace left = hotPlaces.get(i);
                HotPlace right = hotPlaces.get(j);
                if (left.getScore() < right.getScore()) {
                    hotPlaces.set(i, right);
                    hotPlaces.set(j, left);
                }
            }
        }
    }

    public static final class HotPlaceResult {
        private final ArrayList<HotPlace> allPlaces;
        private final ArrayList<HotPlace> attractionPlaces;
        private final ArrayList<HotPlace> foodPlaces;

        private HotPlaceResult(ArrayList<HotPlace> allPlaces, ArrayList<HotPlace> attractionPlaces,
                               ArrayList<HotPlace> foodPlaces) {
            this.allPlaces = allPlaces;
            this.attractionPlaces = attractionPlaces;
            this.foodPlaces = foodPlaces;
        }

        public static HotPlaceResult empty() {
            return new HotPlaceResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        public ArrayList<HotPlace> getAllPlaces() {
            return allPlaces;
        }

        public ArrayList<HotPlace> getAttractionPlaces() {
            return attractionPlaces;
        }

        public ArrayList<HotPlace> getFoodPlaces() {
            return foodPlaces;
        }

        private String countSummary() {
            return "전체 " + allPlaces.size()
                    + " / 명소 " + attractionPlaces.size()
                    + " / 먹거리 " + foodPlaces.size();
        }
    }
}
