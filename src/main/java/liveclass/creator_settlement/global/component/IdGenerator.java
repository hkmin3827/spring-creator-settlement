package liveclass.creator_settlement.global.component;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdGenerator {
    private final AtomicLong saleRecordCounter = new AtomicLong(20);
    private final AtomicLong cancelRecordCounter = new AtomicLong(20);
    private final AtomicLong settlementCounter = new AtomicLong(10);

    public String generateSaleRecordId() {
        return "sale-" + saleRecordCounter.getAndIncrement();
    }
    public String generateSettlementId() {
        return "settlement-" + settlementCounter.getAndIncrement();
    }
    public String generateCancelRecordId() {
        return "cancel-" + cancelRecordCounter.getAndIncrement();
    }
}
