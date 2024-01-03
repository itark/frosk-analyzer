package nu.itark.frosk.bot.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.itark.frosk.bot.bot.dto.trade.OrderTypeDTO;
import nu.itark.frosk.bot.bot.util.base.domain.BaseDomain;
import nu.itark.frosk.bot.bot.util.java.EqualsBuilder;
import nu.itark.frosk.bot.bot.util.jpa.CurrencyAmount;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import java.time.ZonedDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * Trade (map "TRADES" table).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "TRADES")
public class Trade extends BaseDomain {

    /** Technical ID. */
    @Id
    @Column(name = "UID")
    @GeneratedValue(strategy = IDENTITY)
    private Long uid;

    /** An identifier set by the exchange that uniquely identifies the trade. */
    @Column(name = "TRADE_ID")
    private String tradeId;

    /** Trade type i.e. bid (buy) or ask (sell). */
    @Enumerated(STRING)
    @Column(name = "TYPE")
    private OrderTypeDTO type;

    /** The order responsible for this trade. */
    @ManyToOne
    @JoinColumn(name = "FK_ORDER_UID", nullable = false)
    private Order order;

    /** Currency pair. */
    @Column(name = "CURRENCY_PAIR")
    private String currencyPair;

    /** Amount that was ordered. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "AMOUNT_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "AMOUNT_CURRENCY"))
    })
    private CurrencyAmount amount;

    /** The price. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "PRICE_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "PRICE_CURRENCY"))
    })
    private CurrencyAmount price;

    /** The fee that was charged by the exchange for this trade. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "FEE_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "FEE_CURRENCY"))
    })
    private CurrencyAmount fee;

    /** An identifier provided by the user on placement that uniquely identifies the order of this trade. */
    @Column(name = "USER_REFERENCE")
    private String userReference;

    /** The timestamp of the trade. */
    @Column(name = "TIMESTAMP")
    private ZonedDateTime timestamp;

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        final Trade that = (Trade) o;
        return new EqualsBuilder()
                .append(this.uid, that.uid)
                .append(this.tradeId, that.tradeId)
                .append(this.type, that.type)
                .append(this.currencyPair, that.currencyPair)
                .append(this.amount, that.amount)
                .append(this.price, that.price)
                .append(this.fee, that.fee)
                .append(this.userReference, that.userReference)
                .append(this.timestamp, that.timestamp)
                .isEquals();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(tradeId)
                .toHashCode();
    }

}
