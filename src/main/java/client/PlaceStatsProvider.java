package client;

import java.io.IOException;

public interface PlaceStatsProvider {
    boolean hasApiKey();

    GooglePlacesClient.PlaceStats findPlaceStats(String placeName) throws IOException, InterruptedException;
}
