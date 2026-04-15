package liveclass.creator_settlement.domain.cancelRecord;

import liveclass.creator_settlement.app.settlement.dto.CancelAggregationDto;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    @Query("""
        SELECT cr FROM CancelRecord cr
        JOIN SaleRecord sr ON cr.saleRecordId = sr.id
        JOIN Course c ON sr.courseId = c.id
        WHERE c.creatorId = :creatorId
        AND cr.cancelledAt >= :start AND cr.cancelledAt <= :end
        """)
    List<CancelRecord> findByCreatorIdAndCancelledAtBetween(
            @Param("creatorId") String creatorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new liveclass.creator_settlement.app.settlement.dto.CancelAggregationDto(
            SUM(CASE WHEN sr.paidAt >= :start AND sr.paidAt <= :end THEN cr.refundAmount ELSE null END),
            COUNT(cr.id)
        )
        FROM CancelRecord cr
        JOIN SaleRecord sr ON cr.saleRecordId = sr.id
        JOIN Course c ON sr.courseId = c.id
        WHERE c.creatorId = :creatorId
        AND cr.cancelledAt >= :start AND cr.cancelledAt <= :end
        """)
    CancelAggregationDto aggregateCancelsForSettlement(
            @Param("creatorId") String creatorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto(
            c.creatorId, SUM(cr.refundAmount)
        )
        FROM CancelRecord cr
        JOIN SaleRecord sr ON sr.id = cr.saleRecordId
        JOIN Course c ON c.id = sr.courseId
        WHERE cr.cancelledAt >= :start AND cr.cancelledAt <= :end
        GROUP BY c.creatorId
        """)
    List<CreatorAggregationDto> aggregateCancelsByCreatorInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
