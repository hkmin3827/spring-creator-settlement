package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.app.settlement.dto.MonthlySettlementRes;
import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettlementQueryService {

    private final SettlementRepository settlementRepository;
    private final CreatorQueryService creatorQueryService;
    private final SettlementService settlementService;

    public MonthlySettlementRes getMonthlySettlement(String creatorId, YearMonth yearMonth) {
        String creatorName = creatorQueryService.getCreatorName(creatorId);

        if (YearMonth.now().isBefore(yearMonth)) {
            throw new BusinessException(ErrorCode.YEAR_MONTH_BAD_REQUEST);
        }
        // 현재 월: 항상 실시간 계산 (정산 존재 X)
        if (YearMonth.now().equals(yearMonth)) {
            SettlementCalculation calc = settlementService.calculate(creatorId, yearMonth);
            return new MonthlySettlementRes(
                    creatorId, creatorName, yearMonth.toString(), SettlementStatus.PENDING,
                    calc.totalAmount(), calc.refundAmount(), calc.netAmount(),
                    calc.commissionRate(), calc.strippedCommissionAmount(), calc.expectedSettleAmount(),
                    calc.sellCount(), calc.cancelCount()
            );
        }

        // 과거 월: 배치가 완료됐으면 Settlement에서 반환, 아직 안 돌았으면 실시간 계산
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth.toString())
                .map(settlement -> MonthlySettlementRes.from(settlement, creatorName))
                .orElseGet(() -> {
                    SettlementCalculation calc = settlementService.calculate(creatorId, yearMonth);
                    log.warn("WARN! SETTLEMENT_NOT_FOUND [과거 정산 조회 실패] - 관리자 확인 후 수동 생성/확정 필요: creatorId: {}, yearMonth: {}", creatorId, yearMonth);
                    return new MonthlySettlementRes(
                            creatorId, creatorName, yearMonth.toString(), SettlementStatus.PENDING,
                            calc.totalAmount(), calc.refundAmount(), calc.netAmount(),
                            calc.commissionRate(), calc.strippedCommissionAmount(), calc.expectedSettleAmount(),
                            calc.sellCount(), calc.cancelCount()
                    );
        });
    }
}
