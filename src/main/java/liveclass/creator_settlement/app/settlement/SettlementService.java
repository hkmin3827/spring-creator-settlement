package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.app.settlement.dto.SettlementRes;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementLog;
import liveclass.creator_settlement.domain.settlement.SettlementLogRepository;
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
    private final SettlementLogRepository settlementLogRepository;
    private final CreatorQueryService creatorQueryService;
    private final IdGenerator idGenerator;
    private final SettlementQueryService settlementQueryService;

    public SettlementRes confirm(String creatorId, YearMonth yearMonth) {
        if (!YearMonth.now().isAfter(yearMonth)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_MONTH_NOT_ENDED);
        }

        boolean alreadyConfirmed = settlementRepository.existsByCreatorIdAndYearMonthAndStatusIn(
                creatorId, yearMonth.toString(),
                List.of(SettlementStatus.CONFIRMED, SettlementStatus.PAID)
        );
        if (alreadyConfirmed) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        SettlementCalculation calc = settlementQueryService.calculate(creatorId, yearMonth);

        Settlement settlement = Settlement.create(
                idGenerator.generateSettlementId(),
                creatorId,
                yearMonth.toString()
        );
        settlement.confirm();
        settlementRepository.save(settlement);

        SettlementLog log = SettlementLog.of(
                idGenerator.generateSettlementLogId(),
                settlement.id,
                settlement.creatorId,
                settlement.yearMonth,
                calc.totalAmount(),
                calc.refundAmount(),
                calc.netAmount(),
                calc.commissionRate(),
                calc.commissionAmount(),
                calc.expectedSettleAmount(),
                calc.sellCount(),
                calc.cancelCount()
        );
        settlementLogRepository.save(log);

        String creatorName = creatorQueryService.getCreatorName(creatorId);
        return SettlementRes.from(log, SettlementStatus.CONFIRMED, creatorName);
    }

    public SettlementRes markAsPaid(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.INVALID_SETTLEMENT_STATUS);
        }

        settlement.markAsPaid();

        SettlementLog log = settlementLogRepository.findBySettlementId(settlement.id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String creatorName = creatorQueryService.getCreatorName(settlement.creatorId);
        return SettlementRes.from(log, SettlementStatus.PAID, creatorName);
    }
}
