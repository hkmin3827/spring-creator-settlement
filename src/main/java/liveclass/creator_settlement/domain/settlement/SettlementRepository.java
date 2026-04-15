package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, String> {
    Optional<Settlement> findByCreatorIdAndYearMonth(String creatorId, String yearMonth);

    boolean existsByCreatorIdAndYearMonth(String creatorId, String yearMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT sm FROM Settlement sm WHERE sm.creatorId = :creatorId AND sm.yearMonth = :yearMonth")
    Optional<Settlement> findByCreatorIdAndYearMonthWithPessimisticLock(@Param("creatorId") String creatorId, @Param("yearMonth") String yearMonth);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Settlement sm SET sm.status = :newStatus, sm.paidAt = :paidAt WHERE sm.yearMonth = :yearMonth AND sm.status = :oldStatus")
    int bulkUpdateStatus(@Param("yearMonth") String yearMonth, @Param("newStatus") SettlementStatus newStatus, @Param("oldStatus") SettlementStatus oldStatus, @Param("paidAt") LocalDateTime paidAt);
}
