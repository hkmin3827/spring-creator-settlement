package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.OperatorSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OperatorSettlementQueryService {

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final CreatorQueryService creatorQueryService;

    @Value("${app.commission-rate}")
    private BigDecimal commissionRate;

    public OperatorSettlementRes getOperatorAggregate(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        var start = startDate.atStartOfDay();
        var end = endDate.atTime(LocalTime.MAX);

        List<CreatorAggregationDto> saleAggregates = saleRecordRepository.aggregateSalesByCreatorInRange(start, end);
        List<CreatorAggregationDto> cancelAggregates = cancelRecordRepository.aggregateCancelsByCreatorInRange(start, end);

        Map<String, BigDecimal> saleTotals = new HashMap<>();
        for (CreatorAggregationDto row : saleAggregates) {
            saleTotals.put(row.creatorId(), row.totalAmount());
        }

        Map<String, BigDecimal> cancelTotals = new HashMap<>();
        for (CreatorAggregationDto row : cancelAggregates) {
            cancelTotals.put(row.creatorId(), row.totalAmount());
        }

        Map<String, String> creatorNames = creatorQueryService.getAllCreatorNames();
        var allCreatorIds = new ArrayList<>(creatorNames.keySet());

        List<OperatorSettlementRes.CreatorSettlementEntry> entries = new ArrayList<>();
        Money totalSettlement = Money.ZERO;

        for (String cId : allCreatorIds) {
            Money totalAmount = Money.of(saleTotals.getOrDefault(cId, BigDecimal.ZERO));
            Money refundAmount = Money.of(cancelTotals.getOrDefault(cId, BigDecimal.ZERO));
            Money netAmount = totalAmount.subtract(refundAmount);

            Money finalCommissionAmount = Money.of(netAmount.amount().multiply(commissionRate).setScale(0, RoundingMode.DOWN));
            Money expectedSettleAmount = netAmount.subtract(finalCommissionAmount);

            totalSettlement = totalSettlement.add(expectedSettleAmount);

            entries.add(OperatorSettlementRes.CreatorSettlementEntry.of(cId, creatorNames.get(cId), expectedSettleAmount));
        }

        int startPage = (int) pageable.getOffset();
        int endPage = Math.min(startPage + pageable.getPageSize(), entries.size());
        List<OperatorSettlementRes.CreatorSettlementEntry> pageContent =
                startPage >= entries.size() ? List.of() : entries.subList(startPage, endPage);
        Page<OperatorSettlementRes.CreatorSettlementEntry> page = new PageImpl<>(pageContent, pageable, entries.size());

        return OperatorSettlementRes.from(page, totalSettlement.amount());
    }
}
