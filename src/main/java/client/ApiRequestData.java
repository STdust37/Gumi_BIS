package client;

import java.util.HashMap;

public class ApiRequestData {
    private final ApiEndpoint endpoint;
    private final HashMap<String, String> parameters;

    public ApiRequestData(ApiEndpoint endpoint, HashMap<String, String> parameters) {
        this.endpoint = endpoint;
        this.parameters = new HashMap<>(parameters);
    }

    public ApiEndpoint getEndpoint() {
        return endpoint;
    }

    public HashMap<String, String> getParameters() {
        return new HashMap<>(parameters);
    }
}
