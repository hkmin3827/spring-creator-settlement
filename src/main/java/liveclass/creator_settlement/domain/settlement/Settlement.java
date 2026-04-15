package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"creator_id", "year_month"})
)
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

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 6)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false, precision = 6)
    public BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 6)
    public BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "commission_rate", nullable = false, updatable = false, precision = 5, scale = 4)
    public BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 4)
    public BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "settle_amount", nullable = false, precision = 10, scale = 4)
    public BigDecimal settleAmount = BigDecimal.ZERO;

    @Column(name = "sell_count", nullable = false, updatable = false)
    public long sellCount = 0L;

    @Column(name = "cancel_count", nullable = false)
    public long cancelCount = 0L;

    @Column(updatable = false, nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Column(name = "confirmed_at")
    public LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    public LocalDateTime paidAt;

    @Version
    public Long version;

    public static Settlement create(
            String id, String creatorId, String yearMonth,
            BigDecimal totalAmount, BigDecimal refundAmount, BigDecimal netAmount,
            BigDecimal commissionRate, BigDecimal commissionAmount, BigDecimal settleAmount,
            long sellCount, long cancelCount
    ) {
        Settlement sm = new Settlement();
        sm.id = id;
        sm.creatorId = creatorId;
        sm.yearMonth = yearMonth;
        sm.totalAmount = totalAmount;
        sm.refundAmount = refundAmount;
        sm.netAmount = netAmount;
        sm.commissionRate = commissionRate;
        sm.commissionAmount = commissionAmount;
        sm.settleAmount = settleAmount;
        sm.sellCount = sellCount;
        sm.cancelCount = cancelCount;
        sm.status = SettlementStatus.PENDING;

        return sm;
    }

    public void confirm() {
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public void markAsPaid() {
        this.status = SettlementStatus.PAID;
        this.paidAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }


    public void refundAfterYearMonth(BigDecimal refundAmount) {
        BigDecimal changedRefundAmount, changedNetAmount, changedCommissionAmount, changedSettleAmount;
        changedRefundAmount = this.refundAmount.add(refundAmount);
        changedNetAmount = this.netAmount.subtract(refundAmount);
        changedCommissionAmount = changedNetAmount.multiply(commissionRate);
        changedSettleAmount = changedNetAmount.subtract(changedCommissionAmount);

        this.refundAmount = changedRefundAmount;
        this.netAmount = changedNetAmount;
        this.commissionAmount = changedCommissionAmount;
        this.settleAmount = changedSettleAmount;
    }
}
