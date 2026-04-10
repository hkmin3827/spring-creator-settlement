package liveclass.creator_settlement.app.saleRecord.dto;

import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;

import java.time.LocalDateTime;

public record SaleRecordRes(
    String id,
    String courseId,
    String studentId,
    Long amount,
    LocalDateTime paidAt,
    SaleRecordStatus status
) {
    public static SaleRecordRes from(SaleRecord saleRecord) {
        return new SaleRecordRes(
            saleRecord.id,
            saleRecord.courseId,
            saleRecord.studentId,
            saleRecord.amount,
            saleRecord.paidAt,
            saleRecord.status
        );
    }
}
