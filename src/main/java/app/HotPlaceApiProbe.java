package app;

import client.GooglePlacesClient;
import client.NaverSearchHotPlaceClient;
import model.HotPlace;
import service.HotPlaceService;
import util.CredentialProvider;

import java.util.ArrayList;

public class HotPlaceApiProbe {
    public static void main(String[] args) throws Exception {
        printCredentialStatus();

        NaverSearchHotPlaceClient naverClient = new NaverSearchHotPlaceClient();
        GooglePlacesClient googleClient = new GooglePlacesClient();

        System.out.println("[1] 네이버 지역 검색 후보 조회");
        ArrayList<HotPlace> candidates = naverClient.searchHotPlaceCandidates();
        System.out.println("candidateCount=" + candidates.size());
        for (int i = 0; i < Math.min(candidates.size(), 5); i++) {
            HotPlace place = candidates.get(i);
            System.out.println((i + 1) + ". " + place.getName() + " | " + place.getCategory() + " | " + place.getAddress());
        }

        if (!candidates.isEmpty()) {
            HotPlace first = candidates.get(0);
            System.out.println("[2] 네이버 블로그 최근 언급량 조회: " + first.getName());
            NaverSearchHotPlaceClient.BlogMentionProbeResult mentionResult =
                    naverClient.probeRecentBlogMentions(first.getName());
            System.out.println("blogReturnedItems=" + mentionResult.getReturnedItems());
            System.out.println("blogParsedDates=" + mentionResult.getParsedDateCount());
            System.out.println("blogLatestPostDate=" + mentionResult.getLatestPostDate());
            System.out.println("blogActual30Days=" + mentionResult.getActualRecentCount());
            System.out.println("blogMentionCountUsed=" + mentionResult.getSelectedCount());

            System.out.println("[3] Google Places 평점/리뷰 조회: " + first.getName());
            try {
                GooglePlacesClient.PlaceStats stats = googleClient.findPlaceStats(first.getName());
                System.out.println("googleReviewCount=" + stats.getReviewCount());
                System.out.println("googleRating=" + stats.getRating());
            } catch (Exception e) {
                System.out.println("googleError=" + e.getMessage());
            }
        }

        System.out.println("[4] 통합 핫플 점수 계산");
        HotPlaceService service = new HotPlaceService(naverClient, googleClient);
        ArrayList<HotPlace> topPlaces = service.loadTopHotPlaces();
        System.out.println("status=" + service.getLastLoadMessage());
        for (int i = 0; i < topPlaces.size(); i++) {
            HotPlace place = topPlaces.get(i);
            System.out.println((i + 1) + ". " + place.getName()
                    + " | blog=" + place.getMentionCount()
                    + " | reviews=" + place.getReviewCount()
                    + " | rating=" + place.getRating()
                    + " | score=" + String.format("%.1f", place.getScore()));
        }
    }

    private static void printCredentialStatus() {
        String naverClientId = CredentialProvider.get("NAVER_CLIENT_ID", "naver.client.id");
        String naverClientSecret = CredentialProvider.get("NAVER_CLIENT_SECRET", "naver.client.secret");
        String googleKey = CredentialProvider.get("GOOGLE_PLACES_API_KEY", "google.places.api.key");

        System.out.println("[0] 키 로딩 상태");
        System.out.println("workingDirectory=" + CredentialProvider.describeWorkingDirectory());
        System.out.println("NAVER_CLIENT_ID=" + maskStatus(naverClientId)
                + ", source=" + CredentialProvider.describeSource("NAVER_CLIENT_ID", "naver.client.id"));
        System.out.println("NAVER_CLIENT_SECRET=" + maskStatus(naverClientSecret)
                + ", source=" + CredentialProvider.describeSource("NAVER_CLIENT_SECRET", "naver.client.secret"));
        System.out.println("GOOGLE_PLACES_API_KEY=" + maskStatus(googleKey)
                + ", source=" + CredentialProvider.describeSource("GOOGLE_PLACES_API_KEY", "google.places.api.key"));
    }

    private static String maskStatus(String value) {
        if (value == null || value.isBlank()) {
            return "missing";
        }
        return "present(length=" + value.length() + ")";
    }
}
