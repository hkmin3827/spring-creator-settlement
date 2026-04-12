package liveclass.creator_settlement.domain.cancelRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancel_records")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancelRecord {
    @Id
    public String id;

    @Column(name = "sale_record_id", nullable = false, updatable = false, unique = true)
    public String saleRecordId;

    @Column(precision = 6, updatable = false)
    public BigDecimal refundAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    public LocalDateTime cancelledAt;

    public static CancelRecord of(String id, String saleRecordId, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        CancelRecord record = new CancelRecord();
        record.id = id;
        record.saleRecordId = saleRecordId;
        record.refundAmount = refundAmount;
        return record;
    }
}
