package liveclass.creator_settlement.global.batch.confirm;

import liveclass.creator_settlement.domain.settlement.Settlement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class SettlementConfirmItemWriter implements ItemWriter<Settlement> {

    @Override
    public void write(Chunk<? extends Settlement> chunk) {
        for (Settlement settlement : chunk) {
            settlement.confirm();
            log.info("정산 확정 완료 - settlementId: {}, creatorId: {}, yearMonth: {}",
                    settlement.id, settlement.creatorId, settlement.yearMonth);
        }
    }
}
