package cn.edu.distcourse.flashsale.common;

import lombok.Getter;

/**
 * Generic API response wrapper.
 *
 * @param <T> payload type
 */
@Getter
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T payload;

    private ApiResponse(int status, String message, T payload) {
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    public static <T> ApiResponse<T> ok(T payload) {
        return new ApiResponse<>(200, "ok", payload);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null);
    }
}
