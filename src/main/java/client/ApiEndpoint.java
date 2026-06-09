package client;

public class ApiEndpoint {
    private final String baseUrl;
    private final String path;

    public ApiEndpoint(String baseUrl, String path) {
        this.baseUrl = baseUrl;
        this.path = path;
    }

    public String url() {
        return baseUrl + path;
    }

    public String getPath() {
        return path;
    }
}
