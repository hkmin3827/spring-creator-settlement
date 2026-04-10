package liveclass.creator_settlement.domain.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementLogRepository extends JpaRepository<SettlementLog, String> {
    Optional<SettlementLog> findBySettlementId(String settlementId);
}
