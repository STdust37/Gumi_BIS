package client;

public class NaverSearchEndpointCatalog {
    private static final String BASE_URL = "https://openapi.naver.com";

    public ApiEndpoint localSearch() {
        return new ApiEndpoint(BASE_URL, "/v1/search/local.json");
    }

    public ApiEndpoint blogSearch() {
        return new ApiEndpoint(BASE_URL, "/v1/search/blog.json");
    }
}
