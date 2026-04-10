package liveclass.creator_settlement.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "크리에이터를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    SALE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "판매 내역을 찾을 수 없습니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."),

    ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 판매 내역입니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 월의 정산이 이미 확정되었습니다."),
    INVALID_SETTLEMENT_STATUS(HttpStatus.BAD_REQUEST, "정산 상태 변경이 불가합니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "동시 요청으로 처리가 지연되었습니다. 다시 시도해 주세요."),
    INVALID_MONEY_ARITHMETIC(HttpStatus.UNPROCESSABLE_CONTENT, "잘못된 금액 계산 요청입니다: 결과값이 음수이거나 제약 조건을 위반했습니다.");

    public final HttpStatus status;
    public final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
