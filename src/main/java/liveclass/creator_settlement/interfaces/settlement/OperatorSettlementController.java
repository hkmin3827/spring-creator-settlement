package liveclass.creator_settlement.interfaces.settlement;

import liveclass.creator_settlement.app.settlement.OperatorSettlementQueryService;
import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.app.settlement.dto.OperatorSettlementReq;
import liveclass.creator_settlement.app.settlement.dto.OperatorSettlementRes;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/operator/settlement")
@RequiredArgsConstructor
public class OperatorSettlementController {
    private final OperatorSettlementQueryService operatorSettlementQueryService;
    private final SettlementService settlementService;

    @GetMapping(version = "v1")
    public OperatorSettlementRes getOperatorAggregate(@ModelAttribute OperatorSettlementReq req) {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = req.startDate() != null ? req.startDate() : lastMonth.atDay(1);
        LocalDate endDate = req.endDate() != null ? req.endDate() : lastMonth.atEndOfMonth();
        return operatorSettlementQueryService.getOperatorAggregate(startDate, endDate);
    }

    @PostMapping(value = "/{settlementId}/pay", version = "v1")
    public ResponseEntity<Void> markAsPaid(@PathVariable String settlementId) {
        settlementService.markAsPaid(settlementId);
        return ResponseEntity.noContent().build();
    }
}
