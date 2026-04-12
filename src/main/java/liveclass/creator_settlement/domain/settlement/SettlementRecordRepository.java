package liveclass.creator_settlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, String> {
    Optional<SettlementRecord> findBySettlementId(String settlementId);
}
