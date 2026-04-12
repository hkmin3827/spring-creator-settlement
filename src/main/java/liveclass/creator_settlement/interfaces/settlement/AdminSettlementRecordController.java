package liveclass.creator_settlement.interfaces.settlement;

import liveclass.creator_settlement.app.settlement.SettlementRecordService;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settlement-record")
public class AdminSettlementRecordController {

    private final SettlementRecordService settlementRecordService;
    private final SettlementRepository settlementRepository;

    // 관리자 수동 정산 내역 생성
    public ResponseEntity<Void> createSettlementRecord(@RequestBody String settlementId) {
        Settlement sm = settlementRepository.findById(settlementId).orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlementRecordService.create(sm);

        return ResponseEntity.noContent().build();
    }
}
