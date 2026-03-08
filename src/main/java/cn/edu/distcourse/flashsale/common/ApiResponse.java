package cn.edu.distcourse.flashsale.common;

import lombok.Getter;

/**
 * 统一接口响应封装类
 *
 * @param <T> 响应数据类型
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

    /** 成功响应 */
    public static <T> ApiResponse<T> ok(T payload) {
        return new ApiResponse<>(200, "ok", payload);
    }

    /** 失败响应 */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null);
    }
}
