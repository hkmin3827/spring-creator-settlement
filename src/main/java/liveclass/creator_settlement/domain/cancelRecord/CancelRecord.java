package liveclass.creator_settlement.domain.cancelRecord;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "cancel_records",
    indexes = {
            @Index(name = "idx_cancel_secord_cancelled_at_sale_record", columnList = "cancelled_at, sale_record_id"),
    }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelRecord {
    @Id
    public String id;

    @Column(name = "sale_record_id", nullable = false, updatable = false, unique = true)
    public String saleRecordId;

    @Column(precision = 6, updatable = false)
    public BigDecimal refundAmount;

    @Column(nullable = false, updatable = false)
    public LocalDateTime paidAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    public LocalDateTime cancelledAt;

    public static CancelRecord of(String id, String saleRecordId, LocalDateTime paidAt, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        CancelRecord record = new CancelRecord();
        record.id = id;
        record.saleRecordId = saleRecordId;
        record.paidAt = paidAt;
        record.refundAmount = refundAmount;
        return record;
    }
}
