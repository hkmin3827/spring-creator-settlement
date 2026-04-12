package liveclass.creator_settlement.interfaces.settlement;

import liveclass.creator_settlement.app.settlement.SettlementQueryService;
import liveclass.creator_settlement.app.settlement.dto.MonthlySettlementRecordRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementQueryService settlementQueryService;

    @GetMapping(value = "/creator/{creatorId}", version = "v1")
    public MonthlySettlementRecordRes getMonthlySettlement(
            @PathVariable String creatorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String yearMonth
    ) {
        return settlementQueryService.getMonthlySettlement(creatorId, YearMonth.parse(yearMonth));
    }
}
