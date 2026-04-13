package liveclass.creator_settlement.app.cancelRecord;

import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordCreateReq;
import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordRes;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.course.CourseRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.SettlementRepository;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CancelRecordService {

    private final CancelRecordRepository cancelRecordRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final SettlementRepository settlementRepository;
    private final CourseRepository courseRepository;
    private final IdGenerator idGenerator;

    public CancelRecordRes register(CancelRecordCreateReq req) {
        SaleRecord saleRecord = saleRecordRepository.findByIdWithPessimisticLock(req.saleRecordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SALE_RECORD_NOT_FOUND));

        if (saleRecord.status == SaleRecordStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }

        CancelRecord cancelRecord = CancelRecord.of(
                idGenerator.generateCancelRecordId(),
                req.saleRecordId(),
                saleRecord.paidAt,
                req.refundAmount(),
                req.cancelledAt()
        );

        String creatorId = courseRepository.findById(saleRecord.courseId)
                .map(c -> c.creatorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        String saledYear = saleRecord.paidAt.toString().substring(0, 4);
        String saledMonth = saleRecord.paidAt.toString().substring(5, 7);
        YearMonth saledAt = YearMonth.parse(saledYear + "-" + saledMonth);

        try{
            if (YearMonth.now().isAfter(saledAt)) {
            Settlement sm = settlementRepository.findByCreatorIdAndYearMonthWithPessimisticLock(creatorId, saledAt.toString())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

            sm.refundAfterYearMonth(req.refundAmount());
            }
        } catch (BusinessException e){
            log.warn("WARN! SETTLEMENT_NOT_FOUND [과거 정산 조회 실패] - 관리자 확인 후 수동 생성/확정 필요: creatorId: {}, yearMonth: {}", creatorId, saledAt);
            throw e;
        }

        saleRecord.cancel();

        return CancelRecordRes.from(cancelRecordRepository.save(cancelRecord));
    }
}
