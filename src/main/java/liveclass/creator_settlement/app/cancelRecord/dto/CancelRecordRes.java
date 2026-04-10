package liveclass.creator_settlement.app.cancelRecord.dto;

import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancelRecordRes(
    String id,
    String saleRecordId,
    BigDecimal refundAmount,
    LocalDateTime cancelledAt
) {
    public static CancelRecordRes from(CancelRecord cancelRecord) {
        return new CancelRecordRes(
            cancelRecord.id,
            cancelRecord.saleRecordId,
            cancelRecord.refundAmount,
            cancelRecord.cancelledAt
        );
    }
}
