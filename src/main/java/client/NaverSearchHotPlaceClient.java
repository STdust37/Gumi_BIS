package client;

import model.HotPlace;
import parser.JsonObjectListParser;
import service.HotPlaceCandidateProvider;
import service.MentionCountProvider;
import util.CredentialProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class NaverSearchHotPlaceClient implements HotPlaceCandidateProvider, MentionCountProvider {
    private static final String LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";
    private static final String BLOG_SEARCH_URL = "https://openapi.naver.com/v1/search/blog.json";
    private static final String[] HOT_PLACE_QUERIES = {
            "구미 가볼만한곳",
            "구미 관광명소",
            "구미 공원",
            "구미 카페 맛집"
    };
    private static final int CANDIDATE_COUNT_PER_QUERY = 8;
    private static final int MAX_CANDIDATE_COUNT = 24;
    private static final int BLOG_DISPLAY_COUNT = 100;
    private static final int RECENT_DAYS = 30;
    private static final DateTimeFormatter POST_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final HttpClient httpClient;

    public NaverSearchHotPlaceClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean hasCredentials() {
        return !clientId().isBlank() && !clientSecret().isBlank();
    }

    public ArrayList<HotPlace> searchHotPlaceCandidates() throws IOException, InterruptedException {
        ensureCredentials();
        ArrayList<HotPlace> candidates = new ArrayList<>();
        HashSet<String> seenKeys = new HashSet<>();

        for (String query : HOT_PLACE_QUERIES) {
            String url = LOCAL_SEARCH_URL
                    + "?query=" + encode(query)
                    + "&display=" + CANDIDATE_COUNT_PER_QUERY
                    + "&start=1"
                    + "&sort=comment";

            String json = sendGet(url);
            ArrayList<HashMap<String, String>> rows = JsonObjectListParser.parseList(json, "items");
            int rank = 1;
            int maxRank = Math.max(rows.size(), 1);
            for (HashMap<String, String> row : rows) {
                HotPlace hotPlace = HotPlace.fromMap(row, rank, maxRank);
                String key = dedupeKey(hotPlace);
                if (!hotPlace.getName().isBlank() && seenKeys.add(key)) {
                    candidates.add(hotPlace);
                }
                rank++;
                if (candidates.size() >= MAX_CANDIDATE_COUNT) {
                    return candidates;
                }
            }
        }
        return candidates;
    }

    private String dedupeKey(HotPlace hotPlace) {
        if (hotPlace.getId() != null && !hotPlace.getId().isBlank()) {
            return hotPlace.getId().trim();
        }
        return (hotPlace.getName() + "|" + hotPlace.getAddress()).replaceAll("\\s+", "").toLowerCase();
    }

    public int countRecentBlogMentions(String placeName) throws IOException, InterruptedException {
        return probeRecentBlogMentions(placeName).getSelectedCount();
    }

    public BlogMentionProbeResult probeRecentBlogMentions(String placeName) throws IOException, InterruptedException {
        ensureCredentials();
        String query = placeName + " 구미";
        String url = BLOG_SEARCH_URL
                + "?query=" + encode(query)
                + "&display=" + BLOG_DISPLAY_COUNT
                + "&start=1"
                + "&sort=date";

        String json = sendGet(url);
        ArrayList<HashMap<String, String>> rows = JsonObjectListParser.parseList(json, "items");
        ArrayList<LocalDate> postDates = new ArrayList<>();
        LocalDate latestPostDate = null;
        for (HashMap<String, String> row : rows) {
            LocalDate postDate = parsePostDate(row.getOrDefault("postdate", ""));
            if (postDate == null) {
                continue;
            }
            postDates.add(postDate);
            if (latestPostDate == null || postDate.isAfter(latestPostDate)) {
                latestPostDate = postDate;
            }
        }

        LocalDate cutoff = LocalDate.now().minusDays(RECENT_DAYS);
        int count = countPostsAfter(postDates, cutoff);
        if (count > 0 || latestPostDate == null) {
            return new BlogMentionProbeResult(rows.size(), postDates.size(), latestPostDate, count, count);
        }

        int latestWindowCount = countPostsAfter(postDates, latestPostDate.minusDays(RECENT_DAYS));
        return new BlogMentionProbeResult(rows.size(), postDates.size(), latestPostDate, count, latestWindowCount);
    }

    private int countPostsAfter(ArrayList<LocalDate> postDates, LocalDate cutoff) {
        int count = 0;
        for (LocalDate postDate : postDates) {
            if (postDate != null && !postDate.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    private String sendGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("X-Naver-Client-Id", clientId())
                .header("X-Naver-Client-Secret", clientSecret())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("네이버 검색 API 요청 실패: status=" + response.statusCode()
                    + ", body=" + snippet(response.body()));
        }
        return response.body();
    }

    private String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240) + "...";
    }

    private void ensureCredentials() throws IOException {
        if (!hasCredentials()) {
            throw new IOException("NAVER_CLIENT_ID/SECRET 설정이 필요합니다.");
        }
    }

    private LocalDate parsePostDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, POST_DATE_FORMATTER);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String clientId() {
        return CredentialProvider.get("NAVER_CLIENT_ID", "naver.client.id");
    }

    private String clientSecret() {
        return CredentialProvider.get("NAVER_CLIENT_SECRET", "naver.client.secret");
    }

    public static final class BlogMentionProbeResult {
        private final int returnedItems;
        private final int parsedDateCount;
        private final LocalDate latestPostDate;
        private final int actualRecentCount;
        private final int selectedCount;

        private BlogMentionProbeResult(int returnedItems, int parsedDateCount, LocalDate latestPostDate,
                                       int actualRecentCount, int selectedCount) {
            this.returnedItems = returnedItems;
            this.parsedDateCount = parsedDateCount;
            this.latestPostDate = latestPostDate;
            this.actualRecentCount = actualRecentCount;
            this.selectedCount = selectedCount;
        }

        public int getReturnedItems() {
            return returnedItems;
        }

        public int getParsedDateCount() {
            return parsedDateCount;
        }

        public LocalDate getLatestPostDate() {
            return latestPostDate;
        }

        public int getActualRecentCount() {
            return actualRecentCount;
        }

        public int getSelectedCount() {
            return selectedCount;
        }
    }
}
