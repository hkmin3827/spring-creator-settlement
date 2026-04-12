package liveclass.creator_settlement.app.settlement;

import liveclass.creator_settlement.app.settlement.dto.SettlementCalculation;
import liveclass.creator_settlement.app.settlement.dto.MonthlySettlementRecordRes;
import liveclass.creator_settlement.app.creator.CreatorQueryService;
import liveclass.creator_settlement.domain.settlement.SettlementRecord;
import liveclass.creator_settlement.domain.settlement.SettlementRecordRepository;
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
    private final SettlementRecordRepository settlementRecordRepository;
    private final CreatorQueryService creatorQueryService;
    private final SettlementRecordService settlementRecordService;

    public MonthlySettlementRecordRes getMonthlySettlement(String creatorId, YearMonth yearMonth) {
        String creatorName = creatorQueryService.getCreatorName(creatorId);

        if (YearMonth.now().isBefore(yearMonth)) {
            throw new BusinessException(ErrorCode.YEAR_MONTH_BAD_REQUEST);
        }
        // 현재 월은 항상 실시간 계산 — 배치 대상이 아님
        if (YearMonth.now().equals(yearMonth)) {
            SettlementCalculation calc = settlementRecordService.calculate(creatorId, yearMonth);
            return new MonthlySettlementRecordRes(
                    creatorId, creatorName, yearMonth.toString(), SettlementStatus.PENDING,
                    calc.totalAmount(), calc.refundAmount(), calc.netAmount(),
                    calc.commissionRate(), calc.commissionAmount(), calc.expectedSettleAmount(),
                    calc.sellCount(), calc.cancelCount()
            );
        }

        // 과거 월: 배치가 완료됐으면 SettlementRecord에서 반환, 아직 안 돌았으면 실시간 계산
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth.toString())
                .map(settlement -> {
                    SettlementRecord record = settlementRecordRepository.findBySettlementId(settlement.id)
                            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));
                    return MonthlySettlementRecordRes.from(record, settlement.status, creatorName);
                })
                .orElseGet(() -> {
                    SettlementCalculation calc = settlementRecordService.calculate(creatorId, yearMonth);
                    log.warn("WARN! SETTLEMENT_NOT_FOUND [과거 정산 조회 실패] - 관리자 확인 후 수동 생성/확정 필요: creatorId: {}, yearMonth: {}", creatorId, yearMonth);
                    return new MonthlySettlementRecordRes(
                            creatorId, creatorName, yearMonth.toString(), SettlementStatus.PENDING,
                            calc.totalAmount(), calc.refundAmount(), calc.netAmount(),
                            calc.commissionRate(), calc.commissionAmount(), calc.expectedSettleAmount(),
                            calc.sellCount(), calc.cancelCount()
                    );
        });
    }
}
