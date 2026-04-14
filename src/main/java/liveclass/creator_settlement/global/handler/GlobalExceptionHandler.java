package liveclass.creator_settlement.global.handler;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllUncaughtException(Exception e) {
        log.error("Unhandled Exception Occurred: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해보주세요. 재시도 실패 시 관리자에게 문의해주세요."));
    }

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

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDateTimeParseException(DateTimeParseException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ErrorCode.INVALID_DATE_TIME_FORM.message));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockException() {
        return ResponseEntity
            .status(ErrorCode.CONCURRENT_UPDATE_CONFLICT.status)
            .body(Map.of("message", ErrorCode.CONCURRENT_UPDATE_CONFLICT.message));
    }

    @ExceptionHandler(PessimisticLockException.class)
    public ResponseEntity<Map<String, String>> handlePessimisticLockException() {
        return ResponseEntity
            .status(ErrorCode.LOCK_ACQUISITION_FAILED.status)
            .body(Map.of("message", ErrorCode.LOCK_ACQUISITION_FAILED.message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException() {
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_FORM.status)
                .body(Map.of("message", ErrorCode.INVALID_INPUT_FORM.message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        if (YearMonth.class.equals(e.getRequiredType())) {
            return ResponseEntity
                    .status(ErrorCode.INVALID_INPUT_FORM.status)
                    .body(Map.of("message", ErrorCode.INVALID_INPUT_FORM.message));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ErrorCode.INVALID_INPUT_TYPE.message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException: ", e);
        return ResponseEntity
                .status(ErrorCode.DB_CONSTRAINT_VIOLATION.status)
                .body(Map.of("message", ErrorCode.DB_CONSTRAINT_VIOLATION.message));
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<Map<String, String>> handleQueryTimeoutException(QueryTimeoutException e) {
        log.warn("QueryTimeoutException: ", e);
        return ResponseEntity
                .status(ErrorCode.QUERY_TIMEOUT.status)
                .body(Map.of("message", ErrorCode.QUERY_TIMEOUT.message));
    }
}
