package liveclass.creator_settlement.interfaces.settlement;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.settlement.AdminSettlementQueryService;
import liveclass.creator_settlement.app.settlement.SettlementService;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementReq;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementRes;
import liveclass.creator_settlement.app.settlement.dto.SettlementReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settlement")
@RequiredArgsConstructor
public class AdminSettlementController {
    private final AdminSettlementQueryService adminSettlementQueryService;
    private final SettlementService settlementService;

    @GetMapping(version = "v1")
    public AdminSettlementRes getAdminAggregate(@Valid @ModelAttribute AdminSettlementReq req) {
        return adminSettlementQueryService.getAdminAggregate(req.startDate(), req.endDate());
    }

    // confirm -> paid : 정산 결제 후 호출
    @PostMapping(value = "/{settlementId}/pay", version = "v1")
    public ResponseEntity<Void> markAsPaid(@PathVariable String settlementId) {
        settlementService.markAsPaid(settlementId);
        return ResponseEntity.noContent().build();
    }

    // 관리자 수동 생성 메서드
    @PostMapping(value = "/create", version = "v1")
    public ResponseEntity<Void> create(@RequestBody @Valid SettlementReq req) {
        settlementService.createPending(req.creatorId(), req.yearMonth().toString());
        return ResponseEntity.noContent().build();
    }

    // 관리자 수동 컨펌 메서드
    @PostMapping(value = "/confirm", version = "v1")
    public ResponseEntity<Void> confirm(@RequestBody @Valid SettlementReq req) {
        String settlementId = settlementService.createPending(req.creatorId(), req.yearMonth().toString());
        settlementService.confirmPending(settlementId);
        return ResponseEntity.noContent().build();
    }
}
