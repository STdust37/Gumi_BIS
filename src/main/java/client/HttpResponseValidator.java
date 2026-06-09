package client;

import java.io.IOException;

public class HttpResponseValidator {
    public void requireSuccess(ApiResponseData response, String sourceName) throws IOException {
        if (response == null || !response.isSuccess()) {
            int status = response == null ? -1 : response.getStatusCode();
            throw new ApiException(sourceName + " request failed: status=" + status);
        }
    }
}
