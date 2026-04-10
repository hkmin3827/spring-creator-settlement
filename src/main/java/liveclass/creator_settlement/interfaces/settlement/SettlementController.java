package liveclass.creator_settlement.interfaces.settlement;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.settlement.SettlementQueryService;
import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementReq;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.SettlementRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement")
public class SettlementController {

    private final SettlementQueryService settlementQueryService;
    private final SettlementService settlementService;

    @GetMapping(value = "/creator/{creatorId}", version = "v1")
    public SettlementRes getMonthlySettlement(
            @PathVariable String creatorId,
            @RequestParam String yearMonth
    ) {
        return settlementQueryService.getMonthlySettlement(creatorId, YearMonth.parse(yearMonth));
    }

    @PostMapping(value = "/confirm", version = "v1")
    public SettlementRes confirm(
            @RequestParam String creatorId,
            @RequestParam String yearMonth
    ) {
        return settlementService.confirm(creatorId, YearMonth.parse(yearMonth));
    }

    @PostMapping(value = "/{settlementId}/pay", version = "v1")
    public SettlementRes markAsPaid(@PathVariable String settlementId) {
        return settlementService.markAsPaid(settlementId);
    }
}
