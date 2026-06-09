package client;

public class GooglePlacesEndpointCatalog {
    private static final String BASE_URL = "https://places.googleapis.com";

    public ApiEndpoint textSearch() {
        return new ApiEndpoint(BASE_URL, "/v1/places:searchText");
    }
}
