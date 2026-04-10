package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.SettlementRes;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final CreatorQueryService creatorQueryService;
    private final IdGenerator idGenerator;
    private final SettlementQueryService settlementQueryService;

    public SettlementRes confirm(String creatorId, YearMonth yearMonth) {
        boolean alreadyConfirmed = settlementRepository.existsByCreatorIdAndYearMonthAndStatusIn(
                creatorId, yearMonth.toString(),
                List.of(SettlementStatus.CONFIRMED, SettlementStatus.PAID)
        );
        if (alreadyConfirmed) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        SettlementQueryService.SettlementCalculation calc =
                settlementQueryService.calculate(creatorId, yearMonth);

        Settlement settlement = Settlement.confirm(
                idGenerator.generateSettlementId(),
                creatorId,
                yearMonth.toString(),
                calc.totalAmount(),
                calc.refundAmount(),
                calc.netAmount(),
                calc.commissionRate(),
                calc.commissionAmount(),
                calc.expectedSettleAmount(),
                calc.sellCount(),
                calc.cancelCount()
        );

        settlementQueryService.evictPendingCache(creatorId, yearMonth);

        String creatorName = creatorQueryService.getCreatorName(creatorId);
        return SettlementRes.from(settlementRepository.save(settlement), creatorName);
    }

    public SettlementRes markAsPaid(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.markAsPaid();
        String creatorName = creatorQueryService.getCreatorName(settlement.creatorId);
        return SettlementRes.from(settlement, creatorName);
    }
}
