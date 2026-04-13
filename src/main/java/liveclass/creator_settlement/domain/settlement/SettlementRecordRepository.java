package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, String> {
    Optional<SettlementRecord> findBySettlementId(String settlementId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT smr FROM SettlementRecord smr WHERE smr.creatorId = :creatorId AND smr.yearMonth = :yearMonth")
    Optional<SettlementRecord> findByCreatorIdAndYearMonthWithPessimisticLock(@Param("creatorId") String creatorId, @Param("yearMonth") String yearMonth);
}
