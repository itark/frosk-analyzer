package nu.itark.frosk.bot.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.itark.frosk.bot.bot.dto.trade.OrderStatusDTO;
import nu.itark.frosk.bot.bot.dto.trade.OrderTypeDTO;
import nu.itark.frosk.bot.bot.util.base.domain.BaseDomain;
import nu.itark.frosk.bot.bot.util.java.EqualsBuilder;
import nu.itark.frosk.bot.bot.util.jpa.CurrencyAmount;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.IDENTITY;


/**
 * Order (map "ORDERS" table).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "ORDERS")
public class Order extends BaseDomain {

    /** Technical ID. */
    @Id
    @Column(name = "UID")
    @GeneratedValue(strategy = IDENTITY)
    private Long uid;

    /** An identifier set by the exchange that uniquely identifies the order. */
    @Column(name = "ORDER_ID")
    private String orderId;

    /** Order type i.e. bid (buy) or ask (sell). */
    @Enumerated(STRING)
    @Column(name = "TYPE")
    private OrderTypeDTO type;

    /** The strategy that created the order. */
    @ManyToOne(fetch = EAGER)
    @JoinColumn(name = "FK_STRATEGY_UID", updatable = false)
    private Strategy strategy;

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

    /** Weighted Average price of the fills in the order. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "AVERAGE_PRICE_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "AVERAGE_PRICE_CURRENCY"))
    })
    private CurrencyAmount averagePrice;

    /** Limit price. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "LIMIT_PRICE_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "LIMIT_PRICE_CURRENCY"))
    })
    private CurrencyAmount limitPrice;

    /** Market price - The price Cassandre had when the order was created. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "MARKET_PRICE_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "MARKET_PRICE_CURRENCY"))
    })
    private CurrencyAmount marketPrice;

    /** The leverage to use for margin related to this order. */
    @Column(name = "LEVERAGE")
    private String leverage;

    /** Order status. */
    @Enumerated(STRING)
    @Column(name = "STATUS")
    private OrderStatusDTO status;

    /** Amount to be ordered / amount that has been matched against order on the order book/filled. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "CUMULATIVE_AMOUNT_VALUE")),
            @AttributeOverride(name = "currency", column = @Column(name = "CUMULATIVE_AMOUNT_CURRENCY"))
    })
    private CurrencyAmount cumulativeAmount;

    /** An identifier provided by the user on placement that uniquely identifies the order. */
    @Column(name = "USER_REFERENCE")
    private String userReference;

    /** The timestamp of the order. */
    @Column(name = "TIMESTAMP")
    private ZonedDateTime timestamp;

    /** All trades related to order. */
    @OneToMany(mappedBy = "order", fetch = EAGER)
    @OrderBy("timestamp")
    @ToString.Exclude
    private Set<Trade> trades = new LinkedHashSet<>();

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        final Order that = (Order) o;
        return new EqualsBuilder()
                .append(this.uid, that.uid)
                .append(this.orderId, that.orderId)
                .append(this.type, that.type)
                .append(this.currencyPair, that.currencyPair)
                .append(this.amount, that.amount)
                .append(this.averagePrice, that.averagePrice)
                .append(this.limitPrice, that.limitPrice)
                .append(this.marketPrice, that.marketPrice)
                .append(this.leverage, that.leverage)
                .append(this.status, that.status)
                .append(this.cumulativeAmount, that.cumulativeAmount)
                .append(this.userReference, that.userReference)
                .append(this.timestamp, that.timestamp)
                .isEquals();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(orderId)
                .toHashCode();
    }

}
