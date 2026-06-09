package client;

import parser.JsonObjectListParser;
import util.CredentialProvider;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class GooglePlacesClient implements PlaceStatsProvider {
    private static final String TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,places.rating,places.userRatingCount";

    private final HttpClient httpClient;

    public GooglePlacesClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public boolean hasApiKey() {
        return !apiKey().isBlank();
    }

    public PlaceStats findPlaceStats(String placeName) throws IOException, InterruptedException {
        if (!hasApiKey()) {
            return PlaceStats.empty();
        }

        String body = "{\"textQuery\":\"" + escapeJson(placeName + " 구미") + "\","
                + "\"languageCode\":\"ko\","
                + "\"regionCode\":\"KR\","
                + "\"maxResultCount\":1}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TEXT_SEARCH_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("X-Goog-Api-Key", apiKey())
                .header("X-Goog-FieldMask", FIELD_MASK)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google Places 요청 실패: status=" + response.statusCode()
                    + ", body=" + snippet(response.body()));
        }

        ArrayList<HashMap<String, String>> rows = JsonObjectListParser.parseList(response.body(), "places");
        if (rows.isEmpty()) {
            return PlaceStats.empty();
        }
        HashMap<String, String> first = rows.get(0);
        return new PlaceStats(parseInt(first.getOrDefault("userRatingCount", "")),
                parseDouble(first.getOrDefault("rating", "")));
    }

    private String apiKey() {
        return CredentialProvider.get("GOOGLE_PLACES_API_KEY", "google.places.api.key");
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String snippet(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 240 ? compact : compact.substring(0, 240) + "...";
    }

    public static final class PlaceStats {
        private final int reviewCount;
        private final double rating;

        private PlaceStats(int reviewCount, double rating) {
            this.reviewCount = reviewCount;
            this.rating = rating;
        }

        public static PlaceStats empty() {
            return new PlaceStats(0, 0.0);
        }

        public int getReviewCount() {
            return reviewCount;
        }

        public double getRating() {
            return rating;
        }
    }
}
