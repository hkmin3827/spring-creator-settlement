package liveclass.creator_settlement.app.cancelRecord;

import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordCreateReq;
import liveclass.creator_settlement.app.cancelRecord.dto.CancelRecordRes;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecord;
import liveclass.creator_settlement.domain.cancelRecord.CancelRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.domain.saleRecord.constant.SaleRecordStatus;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelRecordService {

    private final CancelRecordRepository cancelRecordRepository;
    private final SaleRecordRepository saleRecordRepository;
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
                req.refundAmount(),
                req.cancelledAt()
        );

        saleRecord.cancel();

        return CancelRecordRes.from(cancelRecordRepository.save(cancelRecord));
    }
}
