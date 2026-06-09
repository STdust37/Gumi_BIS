package client;

import java.io.IOException;

public interface ApiClient {
    String get(String url) throws IOException, InterruptedException;

    String post(String url, String body, String contentType) throws IOException, InterruptedException;
}
