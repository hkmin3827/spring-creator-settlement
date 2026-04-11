package liveclass.creator_settlement.global.handler;

import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.YearMonth;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getErrorCode().status)
            .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .findFirst()
            .orElse("입력값이 유효하지 않습니다.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockException() {
        return ResponseEntity
            .status(ErrorCode.CONCURRENT_UPDATE_CONFLICT.status)
            .body(Map.of("message", ErrorCode.CONCURRENT_UPDATE_CONFLICT.message));
    }

    @ExceptionHandler(jakarta.persistence.PessimisticLockException.class)
    public ResponseEntity<Map<String, String>> handlePessimisticLockException() {
        return ResponseEntity
            .status(ErrorCode.LOCK_ACQUISITION_FAILED.status)
            .body(Map.of("message", ErrorCode.LOCK_ACQUISITION_FAILED.message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException() {
        return ResponseEntity
                .status(ErrorCode.INVALID_YEAR_MONTH_INPUT_VALUE.status)
                .body(Map.of("message", ErrorCode.INVALID_YEAR_MONTH_INPUT_VALUE.message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        if (YearMonth.class.equals(e.getRequiredType())) {
            return ResponseEntity
                    .status(ErrorCode.INVALID_YEAR_MONTH_INPUT_VALUE.status)
                    .body(Map.of("message", ErrorCode.INVALID_YEAR_MONTH_INPUT_VALUE.message));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ErrorCode.INVALID_INPUT_TYPE.message));
    }
}
