package liveclass.creator_settlement.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "크리에이터를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    SALE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "판매 내역을 찾을 수 없습니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 로그를 찾을 수 없습니다."),

    ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 판매 내역입니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 월의 정산이 이미 확정되었습니다."),
    INVALID_SETTLEMENT_STATUS(HttpStatus.BAD_REQUEST, "정산 상태 변경이 불가합니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "동시 요청으로 처리가 지연되었습니다. 다시 시도해 주세요."),
    INVALID_MONEY_ARITHMETIC(HttpStatus.UNPROCESSABLE_CONTENT, "잘못된 금액 계산 요청입니다: 결과값이 음수이거나 제약 조건을 위반했습니다."),
    SETTLEMENT_MONTH_NOT_ENDED(HttpStatus.BAD_REQUEST, "아직 종료되지 않은 월의 정산은 확정할 수 없습니다."),
    YEAR_MONTH_BAD_REQUEST(HttpStatus.BAD_REQUEST, "현재 년월보다 이후인 년월 값은 입력될 수 없습니다."),
    INVALID_SALE_RECORD_AMOUNT(HttpStatus.CONFLICT, "강의료 판매료는 기존 강의의 등록된 가격보다 클 수 없습니다."),
    INVALID_STATUS_TO_SETTLE(HttpStatus.CONFLICT, "확정되지 않은 정산 내역이거나 이미 지불된 정산 내역입니다."),
    ALREADY_CONFIRMED_SETTLEMENT(HttpStatus.CONFLICT, "이미 확인된 정산입니다."),
    ALREADY_PAID_SETTLEMENT(HttpStatus.CONFLICT, "이미 정산이 완료되었습니다."),
    INVALID_YEAR_MONTH_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 날짜 형식입니다. yyyy-MM과 같은 형식으로 입력해주세요. (ex: 2025-03)"),
    INVALID_INPUT_TYPE(HttpStatus.BAD_REQUEST, "입력 값과 기대 값이 타입이 동일하지 않습니다."),
    CONCURRENT_UPDATE_CONFLICT(HttpStatus.CONFLICT, "동시 요청으로 인해 처리에 실패했습니다. 다시 시도해 주세요.");

    public final HttpStatus status;
    public final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
