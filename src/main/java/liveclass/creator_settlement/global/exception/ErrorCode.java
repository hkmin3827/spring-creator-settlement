package liveclass.creator_settlement.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "크리에이터를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    SALE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "판매 내역을 찾을 수 없습니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 로그를 찾을 수 없습니다."),

    ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 판매 내역입니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 월의 정산이 이미 존재합니다."),
    INVALID_SETTLEMENT_STATUS(HttpStatus.BAD_REQUEST, "정산 상태 변경이 불가합니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "동시 요청으로 처리가 지연되었습니다. 다시 시도해 주세요."),
    INVALID_MONEY_VALUE(HttpStatus.BAD_REQUEST, "모든 금액은 음수일 수 없습니다."),
    INVALID_MONEY_ARITHMETIC(HttpStatus.UNPROCESSABLE_CONTENT, "잘못된 금액 계산 요청입니다: 결과값이 음수이거나 제약 조건을 위반했습니다."),
    SETTLEMENT_MONTH_NOT_ENDED(HttpStatus.BAD_REQUEST, "아직 종료되지 않은 월의 정산은 확정할 수 없습니다."),
    INVALID_SETTLEMENT_QUERY_YEAR_MONTH(HttpStatus.BAD_REQUEST, "현재 년월보다 이후인 년월 값은 입력될 수 없습니다."),
    INVALID_SETTLEMENT_CREATE_OR_CONFIRMED_YEAR_MONTH(HttpStatus.BAD_REQUEST, "현재 년월과 이후인 년월 값은 입력될 수 없습니다."),
    INVALID_SALE_RECORD_AMOUNT(HttpStatus.CONFLICT, "강의료 판매료는 기존 강의의 등록된 가격보다 클 수 없습니다."),
    INVALID_STATUS_TO_SETTLE(HttpStatus.CONFLICT, "확정되지 않은 정산 내역이거나 이미 지불된 정산 내역입니다."),
    ALREADY_CONFIRMED_SETTLEMENT(HttpStatus.CONFLICT, "이미 확정된 정산입니다."),
    ALREADY_PAID_SETTLEMENT(HttpStatus.CONFLICT, "이미 정산이 완료되었습니다."),
    INVALID_INPUT_FORM(HttpStatus.BAD_REQUEST, "잘못된 형식의 입력값입니다. 다시 입력해주세요."),
    INVALID_INPUT_TYPE(HttpStatus.BAD_REQUEST, "입력 값과 기대 값의 타입이 동일하지 않습니다."),
    INVALID_DATE_TIME_FORM(HttpStatus.BAD_REQUEST, "잘못된 날짜 형식입니다. 다시 입력해주세요."),
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "동시 요청으로 인해 처리에 실패했습니다. 다시 시도해 주세요."),
    NO_CONFIRMED_SETTLEMENTS(HttpStatus.NOT_FOUND, "해당 월에 지급 가능한 CONFIRMED 상태의 정산이 없습니다."),
    DB_CONSTRAINT_VIOLATION(HttpStatus.CONFLICT, "DB 제약 조건 위반으로 처리에 실패했습니다."),
    QUERY_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "쿼리 처리 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요."),
    EXPIRED_CANCEL_PARIOD(HttpStatus.CONFLICT, "15일이 지난 판매 내역은 취소/환불 불가능합니다.");

    public final HttpStatus status;
    public final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
