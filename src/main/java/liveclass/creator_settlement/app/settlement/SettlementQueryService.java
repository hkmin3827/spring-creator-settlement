package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.AdminSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.SettlementRes;
import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

    private final SettlementRepository settlementRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final CreatorQueryService creatorQueryService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PENDING_CACHE_PREFIX = "settlement:pending:";
    private static final long CACHE_TTL_HOURS = 1;

    public SettlementRes getMonthlySettlement(String creatorId, YearMonth yearMonth) {
        String creatorName = creatorQueryService.getCreatorName(creatorId);

        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth.toString())
                .map(s -> SettlementRes.from(s, creatorName))
                .orElseGet(() -> getPendingSettlement(creatorId, creatorName, yearMonth));
    }

    public AdminSettlementRes getAdminAggregate(LocalDate startDate, LocalDate endDate) {
        var start = startDate.atStartOfDay();
        var end = endDate.atTime(LocalTime.MAX);

        List<CreatorAggregationDto> saleAggregates = saleRecordRepository.aggregateSalesByCreatorInRange(start, end);
        List<CreatorAggregationDto> cancelAggregates = cancelRecordRepository.aggregateCancelsByCreatorInRange(start, end);

        Map<String, BigDecimal> saleTotals = new HashMap<>();
        Map<String, Long> saleCounts = new HashMap<>();
        for (CreatorAggregationDto row : saleAggregates) {
            saleTotals.put(row.creatorId(), row.totalAmount());
            saleCounts.put(row.creatorId(), row.count());
        }

        Map<String, BigDecimal> cancelTotals = new HashMap<>();
        Map<String, Long> cancelCounts = new HashMap<>();
        for (CreatorAggregationDto row : cancelAggregates) {
            cancelTotals.put(row.creatorId(), row.totalAmount());
            cancelCounts.put(row.creatorId(), row.count());
        }

        var allCreatorIds = new HashSet<String>();
        allCreatorIds.addAll(saleTotals.keySet());
        allCreatorIds.addAll(cancelTotals.keySet());

        Map<String, String> creatorNames = creatorQueryService.getCreatorNames(allCreatorIds);

        List<AdminSettlementRes.CreatorSettlementEntry> entries = new ArrayList<>();
        Money totalSettlement = Money.ZERO;

        for (String cId : allCreatorIds) {
            Money totalAmount = Money.of(saleTotals.getOrDefault(cId, BigDecimal.ZERO));
            Money refundAmount = Money.of(cancelTotals.getOrDefault(cId, BigDecimal.ZERO));
            Money netAmount = totalAmount.subtract(refundAmount);
            Money commissionAmount = Money.of(netAmount.amount().multiply(commissionRate));
            Money expectedSettleAmount = netAmount.subtract(commissionAmount);

            totalSettlement = totalSettlement.add(expectedSettleAmount);

            entries.add(new AdminSettlementRes.CreatorSettlementEntry(
                    cId,
                    creatorNames.get(cId),
                    totalAmount.amount(),
                    refundAmount.amount(),
                    netAmount.amount(),
                    commissionAmount.amount(),
                    expectedSettleAmount.amount(),
                    saleCounts.getOrDefault(cId, 0L),
                    cancelCounts.getOrDefault(cId, 0L)
            ));
        }

        return new AdminSettlementRes(entries, totalSettlement.amount());
    }

    SettlementCalculation calculate(String creatorId, YearMonth yearMonth) {
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

    private SettlementRes getPendingSettlement(String creatorId, String creatorName, YearMonth yearMonth) {
        String cacheKey = PENDING_CACHE_PREFIX + creatorId + ":" + yearMonth;
        SettlementRes cached = (SettlementRes) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        SettlementCalculation calc = calculate(creatorId, yearMonth);
        SettlementRes result = new SettlementRes(
                creatorId, creatorName, yearMonth.toString(), SettlementStatus.PENDING,
                calc.totalAmount(), calc.refundAmount(), calc.netAmount(),
                calc.commissionRate(), calc.commissionAmount(), calc.expectedSettleAmount(),
                calc.sellCount(), calc.cancelCount()
        );

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return result;
    }

    void evictPendingCache(String creatorId, YearMonth yearMonth) {
        redisTemplate.delete(PENDING_CACHE_PREFIX + creatorId + ":" + yearMonth);
    }

    record SettlementCalculation(
            BigDecimal totalAmount,
            BigDecimal refundAmount,
            BigDecimal netAmount,
            BigDecimal commissionRate,
            BigDecimal commissionAmount,
            BigDecimal expectedSettleAmount,
            long sellCount,
            long cancelCount
    ) {}
}
