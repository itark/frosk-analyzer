package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * One insider (PDMR) transaction from Finansinspektionen's insider register (S12).
 * {@code externalId} is a stable hash of the source row used to dedupe re-syncs.
 */
@Entity
@Table(name = "insider_trade")
@Getter
@Setter
@NoArgsConstructor
public class InsiderTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "external_id", unique = true)
    private String externalId;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "pdmr_name")
    private String pdmrName;

    @Column(name = "instrument_name")
    private String instrumentName;

    /** Normalised to "BUY" / "SELL". */
    @Column(name = "transaction_type")
    private String transactionType;

    @Temporal(TemporalType.DATE)
    @Column(name = "transaction_date")
    private Date transactionDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "publication_date")
    private Date publicationDate;

    @Column(name = "volume")
    private Double volume;

    @Column(name = "price")
    private Double price;

    @Column(name = "currency")
    private String currency;
}
