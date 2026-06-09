package parser;

/**
 * JSON 파싱 실패 예외 표현 역할
 */
public class JsonParseException extends RuntimeException {
    public JsonParseException(String message) {
        super(message);
    }
}
