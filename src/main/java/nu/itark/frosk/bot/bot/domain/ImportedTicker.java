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
 * Imported tickers (map "IMPORTED_TICKERS" table).
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "IMPORTED_TICKERS")
public class ImportedTicker {

    /** Technical ID. */
    @Id
    @Column(name = "UID")
    private Long uid;

    /** The currency-pair. */
    @CsvBindByName(column = "CURRENCY_PAIR")
    @Column(name = "CURRENCY_PAIR")
    private String currencyPair;

    /** The opening price is the first trade price that was recorded during the day’s trading. */
    @CsvBindByName(column = "OPEN")
    @Column(name = "OPEN")
    private BigDecimal open;

    /** Last trade field is the price set during the last trade. */
    @CsvBindByName(column = "LAST")
    @Column(name = "LAST")
    private BigDecimal last;

    /** The bid price shown represents the highest bid price. */
    @CsvBindByName(column = "BID")
    @Column(name = "BID")
    private BigDecimal bid;

    /** The ask price shown represents the lowest bid price. */
    @CsvBindByName(column = "ASK")
    @Column(name = "ASK")
    private BigDecimal ask;

    /** The day’s high price. */
    @CsvBindByName(column = "HIGH")
    @Column(name = "HIGH")
    private BigDecimal high;

    /** The day’s low price. */
    @CsvBindByName(column = "LOW")
    @Column(name = "LOW")
    private BigDecimal low;

    /** Volume-weighted average price (VWAP) is the ratio of the value traded to total volume traded over a particular time horizon (usually one day). */
    @CsvBindByName(column = "VWAP")
    @Column(name = "VWAP")
    private BigDecimal vwap;

    /** Volume is the number of shares or contracts traded. */
    @CsvBindByName(column = "VOLUME")
    @Column(name = "VOLUME")
    private BigDecimal volume;

    /** Quote volume. */
    @CsvBindByName(column = "QUOTE_VOLUME")
    @Column(name = "QUOTE_VOLUME")
    private BigDecimal quoteVolume;

    /** The bid size represents the quantity of a security that investors are willing to purchase at a specified bid price. */
    @CsvBindByName(column = "BID_SIZE")
    @Column(name = "BID_SIZE")
    private BigDecimal bidSize;

    /** The ask size represents the quantity of a security that investors are willing to sell at a specified selling price. */
    @CsvBindByName(column = "ASK_SIZE")
    @Column(name = "ASK_SIZE")
    private BigDecimal askSize;

    /** Information timestamp. */
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
        ImportedTicker that = (ImportedTicker) o;
        return Objects.equals(uid, that.uid);
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
                .append(uid)
                .toHashCode();
    }

}
