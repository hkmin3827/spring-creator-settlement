package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementRes;
import liveclass.creator_settlement.domain.creator.CreatorRepository;
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
    private final CreatorRepository creatorRepository;
    private final IdGenerator idGenerator;
    private final SettlementQueryService settlementQueryService;

    public SettlementRes confirm(String creatorId, YearMonth yearMonth) {
        creatorRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATOR_NOT_FOUND));

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
                calc.settlementAmount(),
                calc.sellCount(),
                calc.cancelCount()
        );

        settlementQueryService.evictPendingCache(creatorId, yearMonth);

        return SettlementRes.from(settlementRepository.save(settlement));
    }

    public SettlementRes markAsPaid(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.markAsPaid();
        return SettlementRes.from(settlement);
    }
}
