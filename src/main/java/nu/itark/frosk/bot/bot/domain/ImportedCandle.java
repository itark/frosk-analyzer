package nu.itark.frosk.bot.bot.domain;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO;
import nu.itark.frosk.bot.bot.util.csv.EpochToZonedDateTime;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.Hibernate;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;
import static nu.itark.frosk.bot.bot.dto.util.CurrencyPairDTO.CURRENCY_PAIR_SEPARATOR;

/**
 * Imported candles (map "IMPORTED_CANDLES" table).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "IMPORTED_CANDLES")
public class ImportedCandle {

    /** Technical ID. */
    @Id
    @Column(name = "UID")
    private Long uid;

    /** The currency-pair. */
    @CsvBindByName(column = "CURRENCY_PAIR")
    @Column(name = "CURRENCY_PAIR")
    private String currencyPair;

    /** Opening price (first trade) in the bucket interval. */
    @CsvBindByName(column = "OPEN")
    @Column(name = "OPEN")
    private BigDecimal open;

    /** Highest price during the bucket interval. */
    @CsvBindByName(column = "HIGH")
    @Column(name = "HIGH")
    private BigDecimal high;

    /** Lowest price during the bucket interval. */
    @CsvBindByName(column = "LOW")
    @Column(name = "LOW")
    private BigDecimal low;

    /** Closing price (last trade) in the bucket interval. */
    @CsvBindByName(column = "CLOSE")
    @Column(name = "CLOSE")
    private BigDecimal close;

    /** Volume of trading activity during the bucket interval. */
    @CsvBindByName(column = "VOLUME")
    @Column(name = "VOLUME")
    private BigDecimal volume;

    /** Bucket start time. */
    @CsvCustomBindByName(column = "TIMESTAMP", converter = EpochToZonedDateTime.class)
    @Column(name = "TIMESTAMP")
    private ZonedDateTime timestamp;

    /**
     * Returns currency pair DTO.
     *
     * @return currency pair DTO
     */
    public CurrencyPairDTO getCurrencyPairDTO() {
        if (currencyPair != null) {
            return new CurrencyPairDTO(currencyPair.replaceAll("-", CURRENCY_PAIR_SEPARATOR));
        } else {
            return null;
        }
    }

    /**
     * Setter currencyPair.
     *
     * @param newCurrencyPair the currencyPair to set
     */
    public void setCurrencyPair(final String newCurrencyPair) {
        if (newCurrencyPair != null) {
            currencyPair = newCurrencyPair.replaceAll("-", CURRENCY_PAIR_SEPARATOR);
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
        ImportedCandle that = (ImportedCandle) o;
        return Objects.equals(uid, that.uid);
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(uid)
                .toHashCode();
    }

}
