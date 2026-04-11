package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "settlement_logs",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_log_creator_year_month",columnNames = {"creator_id", "year_month"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLog {

    @Id
    public String id;

    @Column(name = "settlement_id", nullable = false, updatable = false, unique = true)
    public String settlementId;

    @Column(name = "creator_id", nullable = false, updatable = false)
    public String creatorId;

    @Column(name = "year_month", nullable = false, length = 7, updatable = false)
    public String yearMonth;

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 8, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "refund_amount", nullable = false, precision = 8, scale = 2)
    public BigDecimal refundAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 8, scale = 2)
    public BigDecimal netAmount = BigDecimal.ZERO;

    @Column(name = "commission_rate", nullable = false, updatable = false, precision = 5, scale = 4)
    public BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 8, scale = 2)
    public BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "expected_settle_amount", nullable = false, precision = 8, scale = 2)
    public BigDecimal expectedSettleAmount = BigDecimal.ZERO;

    @Column(name = "sell_count", nullable = false, updatable = false)
    public long sellCount = 0L;

    @Column(name = "cancel_count", nullable = false)
    public long cancelCount = 0L;

    @CreationTimestamp
    public LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    public LocalDateTime paidAt;

    public static SettlementLog of(
            String id, String settlementId, String creatorId, String yearMonth,
            BigDecimal totalAmount, BigDecimal refundAmount, BigDecimal netAmount,
            BigDecimal commissionRate, BigDecimal commissionAmount, BigDecimal expectedSettleAmount,
            long sellCount, long cancelCount) {
        SettlementLog log = new SettlementLog();
        log.id = id;
        log.settlementId = settlementId;
        log.creatorId = creatorId;
        log.yearMonth = yearMonth;
        log.totalAmount = totalAmount;
        log.refundAmount = refundAmount;
        log.netAmount = netAmount;
        log.commissionRate = commissionRate;
        log.commissionAmount = commissionAmount;
        log.expectedSettleAmount = expectedSettleAmount;
        log.sellCount = sellCount;
        log.cancelCount = cancelCount;
        return log;
    }


    public void paySuccess() {
        this.paidAt = LocalDateTime.now();
    }
}
