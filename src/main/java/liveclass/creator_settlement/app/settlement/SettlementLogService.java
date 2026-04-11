package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementLog;
import liveclass.creator_settlement.domain.settlement.SettlementLogRepository;
import liveclass.creator_settlement.domain.vo.Money;
import liveclass.creator_settlement.global.component.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.YearMonth;

@RequiredArgsConstructor
@Service
public class SettlementLogService {

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final IdGenerator idGenerator;
    private final SettlementLogRepository settlementLogRepository;

    @Transactional
    public SettlementLog create(Settlement settlement) {

        SettlementCalculation calc = calculate(settlement.creatorId, YearMonth.parse(settlement.yearMonth));
        settlement.confirm();

        SettlementLog log = SettlementLog.of(
                idGenerator.generateSettlementLogId(),
                settlement.id,
                settlement.creatorId,
                settlement.yearMonth,
                calc.totalAmount(),
                calc.refundAmount(),
                calc.netAmount(),
                calc.commissionRate(),
                calc.commissionAmount(),
                calc.expectedSettleAmount(),
                calc.sellCount(),
                calc.cancelCount()
        );

        return settlementLogRepository.save(log);
    }


    @Transactional(readOnly = true)
    public SettlementCalculation calculate(String creatorId, YearMonth yearMonth) {
        var start = yearMonth.atDay(1).atStartOfDay();
        var end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        var sales = saleRecordRepository.findByCreatorIdAndPaidAtBetween(creatorId, start, end);
        var cancels = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(creatorId, start, end);

        Money totalAmount = sales.stream()
                .map(s -> Money.of(s.amount))
                .reduce(Money.ZERO, Money::add);

        Money refundAmount = cancels.stream()
                .map(c -> Money.of(c.refundAmount))
                .reduce(Money.ZERO, Money::add);

        Money netAmount = totalAmount.subtract(refundAmount);
        Money commissionAmount = Money.of(netAmount.amount().multiply(commissionRate));
        Money settlementAmount = netAmount.subtract(commissionAmount);

        return new SettlementCalculation(
                totalAmount.amount(),
                refundAmount.amount(),
                netAmount.amount(),
                commissionRate,
                commissionAmount.amount(),
                settlementAmount.amount(),
                sales.size(),
                cancels.size()
        );
    }
}
