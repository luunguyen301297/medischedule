package vn.medischedule.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/** Mọi lỗi trả về cùng một dạng: { code, message, fields? }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(String code, String message, Map<String, String> fields) {}

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErrorBody(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(), f -> String.valueOf(f.getDefaultMessage()), (a, b) -> a));
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION_ERROR", "Dữ liệu không hợp lệ", fields));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorBody("FORBIDDEN", "Bạn không có quyền thực hiện thao tác này", null));
    }
}
