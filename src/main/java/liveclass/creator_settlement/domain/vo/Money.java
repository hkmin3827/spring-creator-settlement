package liveclass.creator_settlement.domain.vo;

import liveclass.creator_settlement.global.exception.BusinessException;
import liveclass.creator_settlement.global.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(ErrorCode.INVALID_MONEY_VALUE);
    }

    public static Money of(long amount)          { return new Money(BigDecimal.valueOf(amount)); }
    public static Money of(BigDecimal amount)    { return new Money(amount); }
    public static Money of(String amount)        { return new Money(new BigDecimal(amount)); }

    public Money add(Money other)    {
        Objects.requireNonNull(other, "other money must not be null");
        return new Money(this.amount.add(other.amount));
    }
    public Money subtract(Money other) {
        BigDecimal result = this.amount.subtract(other.amount);
        Objects.requireNonNull(other, "other money must not be null");
        if (result.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(ErrorCode.INVALID_MONEY_ARITHMETIC);
        return new Money(result);
    }

    @Override public int compareTo(Money o) { return this.amount.compareTo(o.amount); }
    @Override public String toString()       { return amount.toPlainString(); }
}
