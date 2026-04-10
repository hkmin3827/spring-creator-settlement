package liveclass.creator_settlement.domain.settlement;

import jakarta.persistence.*;
import liveclass.creator_settlement.domain.settlement.constant.SettlementStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {
    @Id
    public String id;

    @Enumerated(EnumType.STRING)
    public SettlementStatus status;

    @Column(precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(precision = 19, scale = 2)
    public BigDecimal netAmount;

    @Column(precision = 19, scale = 2)
    public BigDecimal refundAmount;

    // 수수료는 환경변수로 관리하여 변경 편의성확보하고 git 커밋 & 태그활용으로 이력 추적
    // 정산 예정 금액 서비스로직에서 계산 + redis 저장

    public long sellCount;
    public long cancelCount;
}
