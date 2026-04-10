package liveclass.creator_settlement.domain.cancelRecord;

import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CancelRecordRepository extends JpaRepository<CancelRecord, String> {

    @Query("""
        SELECT cr FROM CancelRecord cr
        WHERE cr.cancelledAt >= :start AND cr.cancelledAt <= :end
        AND cr.saleRecordId IN (
            SELECT sr.id FROM SaleRecord sr
            WHERE sr.courseId IN (SELECT c.id FROM Course c WHERE c.creatorId = :creatorId)
        )
        """)
    List<CancelRecord> findByCreatorIdAndCancelledAtBetween(
            @Param("creatorId") String creatorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT new liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto(
            c.creatorId, SUM(cr.refundAmount), COUNT(cr.id)
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
