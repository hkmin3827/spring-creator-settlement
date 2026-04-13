package liveclass.creator_settlement.global.batch.confirm;

import liveclass.creator_settlement.domain.settlement.Settlement;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class SettlementConfirmItemProcessor implements ItemProcessor<Settlement, Settlement> {

    @Override
    public Settlement process(Settlement settlement) {
        if (settlement.status != SettlementStatus.PENDING) {
            log.info("***이미 확정된 정산 - 스킵*** - settlementId: {}, yearMonth: {}", settlement.id, settlement.yearMonth);
            return null;
        }
        return settlement;
    }
}
