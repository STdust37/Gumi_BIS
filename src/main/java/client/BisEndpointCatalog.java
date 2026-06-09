package client;

public class BisEndpointCatalog {
    private static final String BASE_URL = "https://bis.gumi.go.kr";

    public ApiEndpoint routeList() {
        return new ApiEndpoint(BASE_URL, "/realtime/selectRealtimeRouteList");
    }

    public ApiEndpoint stopList() {
        return new ApiEndpoint(BASE_URL, "/realtime/selectRealtimeBusstopList");
    }

    public ApiEndpoint routeStops() {
        return new ApiEndpoint(BASE_URL, "/realtime/getRealtimeRoute");
    }

    public ApiEndpoint arrivals() {
        return new ApiEndpoint(BASE_URL, "/realtime/getRealtimeArrivalInfo");
    }

    public ApiEndpoint busLocations() {
        return new ApiEndpoint(BASE_URL, "/realtime/getRealtimeBusLoc");
    }
}
