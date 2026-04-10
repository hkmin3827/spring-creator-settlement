package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminSettlementQueryService {

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final CreatorQueryService creatorQueryService;

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

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

        Map<String, String> creatorNames = creatorQueryService.getAllCreatorNames();
        var allCreatorIds = creatorNames.keySet();

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
}
