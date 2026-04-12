package liveclass.creator_settlement.domain.saleRecord;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT sr FROM SaleRecord sr WHERE sr.id = :id")
    Optional<SaleRecord> findByIdWithPessimisticLock(@Param("id") String id);

    @Query("""
        SELECT sr FROM SaleRecord sr
        WHERE sr.courseId IN (SELECT c.id FROM Course c WHERE c.creatorId = :creatorId)
        AND sr.paidAt >= :start AND sr.paidAt <= :end
        ORDER BY sr.paidAt DESC
        """)
    List<SaleRecord> findByCreatorIdAndPaidAtBetween(
            @Param("creatorId") String creatorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT sr FROM SaleRecord sr
        WHERE sr.courseId IN (SELECT c.id FROM Course c WHERE c.creatorId = :creatorId)
        AND sr.paidAt >= :start
        ORDER BY sr.paidAt DESC
        """)
    List<SaleRecord> findByCreatorIdAndPaidAtStart(
            @Param("creatorId") String creatorId,
            @Param("start") LocalDateTime start
    );

    @Query("""
        SELECT sr FROM SaleRecord sr
        WHERE sr.courseId IN (SELECT c.id FROM Course c WHERE c.creatorId = :creatorId)
        AND sr.paidAt <= :end
        ORDER BY sr.paidAt DESC
        """)
    List<SaleRecord> findByCreatorIdAndPaidAtEnd(
            @Param("creatorId") String creatorId,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT sr FROM SaleRecord sr
        WHERE sr.courseId IN (SELECT c.id FROM Course c WHERE c.creatorId = :creatorId)
        ORDER BY sr.paidAt DESC
        """)
    List<SaleRecord> findAllByCreatorId(@Param("creatorId") String creatorId);

    @Query("""
        SELECT new liveclass.creator_settlement.app.settlement.dto.CreatorAggregationDto(
            c.creatorId, SUM(sr.amount), COUNT(sr.id)
        )
        FROM SaleRecord sr JOIN Course c ON c.id = sr.courseId
        WHERE sr.paidAt >= :start AND sr.paidAt <= :end
        GROUP BY c.creatorId
        """)
    List<CreatorAggregationDto> aggregateSalesByCreatorInRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
