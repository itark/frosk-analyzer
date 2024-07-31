package nu.itark.frosk.bot.bot.util.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Currency amount (amount value + currency).
 */
@Getter
@Setter
@RequiredArgsConstructor
@Embeddable
@SuppressWarnings("checkstyle:VisibilityModifier")
public class CurrencyAmount {

    /** Amount value. */
    @Column()
    BigDecimal value;

    /** Amount currency. */
    String currency;

    @Override
    public final String toString() {
        if (value != null) {
            return value + " " + currency;
        } else {
            return "Null";
        }
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        CurrencyAmount that = (CurrencyAmount) o;

        if (value.compareTo(that.getValue()) != 0) {
            return false;
        }
        return Objects.equals(currency, that.currency);
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(value)
                .append(currency)
                .toHashCode();
    }

}
