package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {
    @Id
    public String id;

    @Enumerated(EnumType.STRING)
    public SettlementStatus status;

    @Column(name = "creator_id", nullable = false, updatable = false)
    public String creatorId;

    @Column(name = "year_month", nullable = false, length = 7, updatable = false)
    public String yearMonth;

    @Column(precision = 19, scale = 2, nullable = false)
    public BigDecimal amount;

    @Column(precision = 19, scale = 2, nullable = false)
    public BigDecimal netAmount;

    @Column(precision = 19, scale = 2, nullable = false)
    public BigDecimal refundAmount;

    @Column(name = "commission_rate", precision = 5, scale = 4, nullable = false, updatable = false)
    public BigDecimal commissionRate;

    @Column(name = "commission_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    public BigDecimal commissionAmount;

    @Column(name = "settlement_amount", precision = 19, scale = 2, nullable = false, updatable = false)
    public BigDecimal settlementAmount;

    @CreationTimestamp
    @Column(updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public long sellCount;
    public long cancelCount;

    public static Settlement confirm(
            String id, String creatorId, String yearMonth,
            BigDecimal amount, BigDecimal refundAmount, BigDecimal netAmount,
            BigDecimal commissionRate, BigDecimal commissionAmount, BigDecimal settlementAmount,
            long sellCount, long cancelCount
    ) {
        Settlement s = new Settlement();
        s.id = id;
        s.creatorId = creatorId;
        s.yearMonth = yearMonth;
        s.status = SettlementStatus.CONFIRMED;
        s.amount = amount;
        s.refundAmount = refundAmount;
        s.netAmount = netAmount;
        s.commissionRate = commissionRate;
        s.commissionAmount = commissionAmount;
        s.settlementAmount = settlementAmount;
        s.sellCount = sellCount;
        s.cancelCount = cancelCount;
        return s;
    }

    public void markAsPaid() {
        this.status = SettlementStatus.PAID;
    }
}
