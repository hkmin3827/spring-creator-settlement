package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.SettlementRecordRepository;
import liveclass.creator_settlement.domain.vo.Money;
import liveclass.creator_settlement.global.component.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.YearMonth;

@RequiredArgsConstructor
@Service
public class SettlementRecordService {

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final IdGenerator idGenerator;
    private final SettlementRecordRepository settlementRecordRepository;

    @Transactional
    public SettlementRecord create(Settlement settlement) {

        SettlementCalculation calc = calculate(settlement.creatorId, YearMonth.parse(settlement.yearMonth));

        SettlementRecord record = SettlementRecord.of(
                idGenerator.generateSettlementRecordId(),
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

        return settlementRecordRepository.save(record);
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
        BigDecimal finalCommissionAmount = netAmount.amount().multiply(commissionRate).setScale(0, RoundingMode.DOWN);
        BigDecimal settlementAmount = netAmount.amount().subtract(finalCommissionAmount);

        return new SettlementCalculation(
                totalAmount.amount(),
                refundAmount.amount(),
                netAmount.amount(),
                commissionRate,
                finalCommissionAmount,
                settlementAmount,
                sales.size(),
                cancels.size()
        );
    }
}
