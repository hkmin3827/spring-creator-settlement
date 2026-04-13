package liveclass.creator_settlement.global.batch.confirm;

import liveclass.creator_settlement.domain.settlement.Settlement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettlementConfirmSkipListener implements SkipListener<Settlement, Settlement> {

    @Override
    public void onSkipInWrite(Settlement item, Throwable t) {
        log.error("[확정 배치 최종 실패 !!!] 관리자 수동 후처리 필요 - settlementId: {}, yearMonth: {}, error: {}",
                item.id, item.yearMonth, t.getMessage());
    }

    @Override
    public void onSkipInProcess(Settlement item, Throwable t) {
        log.error("[확정 배치 최종 실패 !!!] processor 처리 실패 - settlementId: {}, error: {}",
                item.id, t.getMessage());
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("[확정 배치 읽기 실패] 정산 조회 실패 - error: {}", t.getMessage());
    }
}
