package liveclass.creator_settlement.interfaces.settlement;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.app.settlement.dto.SettlementReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/{v}/admin/settlement")
@RequiredArgsConstructor
public class AdminSettlementControler {

    private final SettlementService settlementService;

    @PostMapping(value = "/create", version = "v1")
    public ResponseEntity<Void> create(@RequestBody @Valid SettlementReq req) {
        settlementService.createPending(req.creatorId(), YearMonth.parse(req.yearMonth()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/confirm", version = "v1")
    public ResponseEntity<Void> confirm(@RequestBody @Valid SettlementReq req) {
        settlementService.confirmPending(req.creatorId(), YearMonth.parse(req.yearMonth()));
        return ResponseEntity.noContent().build();
    }
}
