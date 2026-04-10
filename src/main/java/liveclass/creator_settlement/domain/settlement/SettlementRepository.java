package liveclass.creator_settlement.domain.settlement;

import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, String> {
    Optional<Settlement> findByCreatorIdAndYearMonth(String creatorId, String yearMonth);

    boolean existsByCreatorIdAndYearMonthAndStatusIn(
            String creatorId, String yearMonth, java.util.List<SettlementStatus> statuses
    );
}
