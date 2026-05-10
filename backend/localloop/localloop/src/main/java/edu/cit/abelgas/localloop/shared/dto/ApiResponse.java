package edu.cit.abelgas.localloop.dto.response;

import java.time.Instant;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetail error;
    private String timestamp;

    public ApiResponse() {}

    public ApiResponse(boolean success, T data, ErrorDetail error, String timestamp) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public ErrorDetail getError() { return error; }
    public String getTimestamp() { return timestamp; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(T data) { this.data = data; }
    public void setError(ErrorDetail error) { this.error = error; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toString());
    }

    public static <T> ApiResponse<T> error(String code, String message, Object details) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(code, message, details),
                Instant.now().toString());
    }

    public static class ErrorDetail {
        private String code;
        private String message;
        private Object details;

        public ErrorDetail() {}

        public ErrorDetail(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Object getDetails() { return details; }
        public void setCode(String code) { this.code = code; }
        public void setMessage(String message) { this.message = message; }
        public void setDetails(Object details) { this.details = details; }
    }
}