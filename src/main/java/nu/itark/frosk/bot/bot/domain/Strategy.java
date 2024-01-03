package nu.itark.frosk.bot.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.itark.frosk.bot.bot.util.base.domain.BaseDomain;
import nu.itark.frosk.bot.bot.util.java.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;

import static jakarta.persistence.GenerationType.IDENTITY;

/**
 * Strategy (map "STRATEGIES" table).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "STRATEGIES")
public class Strategy extends BaseDomain {

    /** Technical ID. */
    @Id
    @Column(name = "UID")
    @GeneratedValue(strategy = IDENTITY)
    private Long uid;

    /** An identifier that uniquely identifies the strategy - Comes from the Java annotation. */
    @Column(name = "STRATEGY_ID")
    private String strategyId;

    /** Strategy name - Comes from the Java annotation. */
    @Column(name = "NAME")
    private String name;

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        final Strategy that = (Strategy) o;
        return new EqualsBuilder()
                .append(this.uid, that.uid)
                .append(this.strategyId, that.strategyId)
                .append(this.name, that.name)
                .isEquals();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(uid)
                .toHashCode();
    }

}
