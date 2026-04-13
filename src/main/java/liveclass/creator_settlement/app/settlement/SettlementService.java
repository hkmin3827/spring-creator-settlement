package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.domain.vo.Money;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final IdGenerator idGenerator;
    private final SettlementRepository settlementRepository;

    public String createPending(String creatorId, String yearMonth) {
        if (!YearMonth.now().isAfter(YearMonth.parse(yearMonth))) {
            throw new BusinessException(ErrorCode.YEAR_MONTH_BAD_REQUEST);
        }

        if (settlementRepository.existsByCreatorIdAndYearMonth(
                creatorId, yearMonth)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }


        SettlementCalculation calc = calculate(creatorId, YearMonth.parse(yearMonth));

        BigDecimal commissionAmount = calc.netAmount().multiply(commissionRate);
        BigDecimal settleAmount = calc.netAmount().subtract(commissionAmount);

        Settlement newSm = Settlement.create(
                idGenerator.generateSettlementId(), creatorId, yearMonth,
                calc.totalAmount(),
                calc.refundAmount(),
                calc.netAmount(),
                calc.commissionRate(),
                commissionAmount,
                settleAmount,
                calc.sellCount(),
                calc.cancelCount()
        );
        settlementRepository.save(newSm);
        return newSm.id;
    }

    public void confirmPending(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status != SettlementStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_CONFIRMED_SETTLEMENT);
        }

        settlement.confirm();
    }

    // 정산금 지급 완료 후 호출 (상태변경) ( CONFIRM -> PAID  + paidAt 저장)
    public void markAsPaid(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status == SettlementStatus.PAID) {
            throw new BusinessException(ErrorCode.ALREADY_PAID_SETTLEMENT);
        }
        if (settlement.status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.markAsPaid();
    }

    public int bulkAsPaidMonthly(YearMonth yearMonth) {
        try {
            int updateCount = settlementRepository.bulkUpdateStatus(yearMonth.toString(), SettlementStatus.PAID, SettlementStatus.CONFIRMED);
            if (updateCount == 0) {
                throw new BusinessException(ErrorCode.NO_CONFIRMED_SETTLEMENTS);
            }
            return updateCount;
        } catch (BusinessException e) {
            throw e;
        } catch (QueryTimeoutException e) {
            throw new BusinessException(ErrorCode.QUERY_TIMEOUT);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.DB_CONSTRAINT_VIOLATION);
        } catch (JpaSystemException e) {
            throw new BusinessException(ErrorCode.DB_CONSTRAINT_VIOLATION);
        }
    }


    @Transactional(readOnly = true)
    public SettlementCalculation calculate(String creatorId, YearMonth yearMonth) {
        var start = yearMonth.atDay(1).atStartOfDay();
        var end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        var sales = saleRecordRepository.findByCreatorIdAndPaidAtBetween(creatorId, start, end, Pageable.ofSize(0)).toList();
        var cancels = cancelRecordRepository.findByCreatorIdAndCancelledAtBetween(creatorId, start, end);

        Money totalAmount = sales.stream()
                .map(s -> Money.of(s.amount))
                .reduce(Money.ZERO, Money::add);

        Money refundAmount = cancels.stream()
                .filter(c -> !c.paidAt.isBefore(start) && !c.paidAt.isAfter(end))
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
