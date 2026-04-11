package liveclass.creator_settlement.app.saleRecord;

import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordCreateReq;
import liveclass.creator_settlement.app.saleRecord.dto.SaleRecordRes;
import liveclass.creator_settlement.domain.course.Course;
import liveclass.creator_settlement.domain.course.CourseRepository;
import liveclass.creator_settlement.domain.saleRecord.SaleRecord;
import liveclass.creator_settlement.domain.saleRecord.SaleRecordRepository;
import liveclass.creator_settlement.global.component.IdGenerator;
import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SaleRecordService {

    private final SaleRecordRepository saleRecordRepository;
    private final CourseRepository courseRepository;
    private final IdGenerator idGenerator;

    public SaleRecordRes register(SaleRecordCreateReq req) {
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));

        if(req.amount().compareTo(course.price) > 0) {
            throw new BusinessException(ErrorCode.INVALID_SALE_RECORD_AMOUNT);
        }

        SaleRecord saleRecord = SaleRecord.of(
                idGenerator.generateSaleRecordId(),
                req.courseId(),
                req.studentId(),
                req.amount(),
                req.paidAt()
        );

        return SaleRecordRes.from(saleRecordRepository.save(saleRecord));
    }

    @Transactional(readOnly = true)
    public List<SaleRecordRes> list(String creatorId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return saleRecordRepository.findByCreatorIdAndPaidAtBetween(
                            creatorId,
                            startDate.atStartOfDay(),
                            endDate.atTime(LocalTime.MAX)
                    ).stream()
                    .map(SaleRecordRes::from)
                    .toList();
        }
        return saleRecordRepository.findAllByCreatorId(creatorId).stream()
                .map(SaleRecordRes::from)
                .toList();
    }
}
