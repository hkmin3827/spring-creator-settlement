package liveclass.creator_settlement.interfaces.settlement;

import jakarta.validation.Valid;
import liveclass.creator_settlement.app.settlement.AdminSettlementQueryService;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementReq;
import liveclass.creator_settlement.app.settlement.dto.AdminSettlementRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settlement")
@RequiredArgsConstructor
public class AdminSettlementController {
    private final AdminSettlementQueryService adminSettlementQueryService;

    @GetMapping(version = "v1")
    public AdminSettlementRes getAdminAggregate(@Valid @ModelAttribute AdminSettlementReq req) {
        return adminSettlementQueryService.getAdminAggregate(req.startDate(), req.endDate());
    }
}
