package client;

import parser.JsonRowsParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class BisClient {
    private static final String BASE_URL = "https://bis.gumi.go.kr";

    private final HttpClient httpClient;
    private final HttpClient certificateFallbackClient;

    public BisClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.certificateFallbackClient = createCertificateFallbackClient();
    }

    public ArrayList<HashMap<String, String>> selectRealtimeRouteList() throws IOException, InterruptedException {
        return postRows("/realtime/selectRealtimeRouteList", new HashMap<>());
    }

    public ArrayList<HashMap<String, String>> selectRealtimeBusstopList() throws IOException, InterruptedException {
        return postRows("/realtime/selectRealtimeBusstopList", new HashMap<>());
    }

    public ArrayList<HashMap<String, String>> getRealtimeRoute(String routeId) throws IOException, InterruptedException {
        HashMap<String, String> params = new HashMap<>();
        params.put("routeId", routeId);
        return postRows("/realtime/getRealtimeRoute", params);
    }

    public ArrayList<HashMap<String, String>> getRealtimeArrivalInfo(String serviceId) throws IOException, InterruptedException {
        HashMap<String, String> params = new HashMap<>();
        params.put("serviceId", serviceId);
        return postRows("/realtime/getRealtimeArrivalInfo", params);
    }

    public ArrayList<HashMap<String, String>> selectDrawRouteList(String routeId) throws IOException, InterruptedException {
        HashMap<String, String> params = new HashMap<>();
        params.put("routeId", routeId);
        return postRows("/realtime/selectDrawRouteList", params);
    }

    public ArrayList<HashMap<String, String>> getRealtimeBusLoc(String routeId) throws IOException, InterruptedException {
        HashMap<String, String> params = new HashMap<>();
        params.put("routeId", routeId);
        return postRows("/realtime/getRealtimeBusLoc", params);
    }

    private ArrayList<HashMap<String, String>> postRows(String path, HashMap<String, String> params) throws IOException, InterruptedException {
        String body = encodeForm(params);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("User-Agent", "Gumi-BIS-Java-Console-MVP/0.1")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("BIS 요청 실패: " + path + " status=" + response.statusCode());
        }
        return JsonRowsParser.parseRows(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (SSLHandshakeException e) {
            if (certificateFallbackClient == null) {
                throw e;
            }
            return certificateFallbackClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }

    private String encodeForm(HashMap<String, String> params) {
        ArrayList<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            pairs.add(key + "=" + value);
        }
        return String.join("&", pairs);
    }

    private HttpClient createCertificateFallbackClient() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());

            return HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .sslContext(sslContext)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            return null;
        }
    }
}
