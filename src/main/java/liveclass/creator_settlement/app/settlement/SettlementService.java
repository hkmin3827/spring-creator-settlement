package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.SettlementRecordRes;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.SettlementRecordRepository;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final CreatorQueryService creatorQueryService;
    private final IdGenerator idGenerator;
    private final SettlementRecordService settlementRecordService;


    public String createPending(String creatorId, String yearMonth) {
        if (!YearMonth.now().isAfter(YearMonth.parse(yearMonth))) {
            throw new BusinessException(ErrorCode.YEAR_MONTH_BAD_REQUEST);
        }

        if (settlementRepository.existsByCreatorIdAndYearMonth(
                creatorId, yearMonth)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        Settlement newSm = Settlement.create(idGenerator.generateSettlementId(), creatorId, yearMonth);
        settlementRepository.save(newSm);
        return newSm.id;
    }

    public SettlementRecordRes confirmPending(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status != SettlementStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_CONFIRMED_SETTLEMENT);
        }

        SettlementRecord record = settlementRecordService.create(settlement);

        settlement.confirm();

        String creatorName = creatorQueryService.getCreatorName(settlement.creatorId);
        return SettlementRecordRes.from(record, SettlementStatus.CONFIRMED, creatorName);
    }

    // 정산금 지급 완료 후 호출 (상태변경) ( CONFIRM -> PAID  + record에 paidAt 저장)
    public void markAsPaid(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status == SettlementStatus.PAID) {
            throw new BusinessException(ErrorCode.ALREADY_PAID_SETTLEMENT);
        }
        if (settlement.status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.markAsPaid();

        SettlementRecord record = settlementRecordRepository.findBySettlementId(settlement.id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_RECORD_NOT_FOUND));
        record.paySuccess();
    }
}
