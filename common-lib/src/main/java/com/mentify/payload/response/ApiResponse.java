package com.mentify.payload.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Integer statusCode;
    private String message;
    private T data;

    @JsonIgnore
    private HttpStatus status;

    public HttpStatus getStatus() {
        if (status != null) {
            return status;
        }

        if (statusCode != null) {
            HttpStatus resolvedStatus = HttpStatus.resolve(statusCode);
            if (resolvedStatus != null) {
                return resolvedStatus;
            }
        }

        return HttpStatus.OK;
    }

    public static <T> ApiResponse<T> success(int statusCode, String message, T data) {
        HttpStatus resolvedStatus = HttpStatus.resolve(statusCode);
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .status(resolvedStatus)
                .build();
    }
}
