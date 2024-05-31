package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "trading_account")
public class TradingAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date createDate;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "account_type_id", referencedColumnName = "id")
	private AccountType accountType;

	@Column(name = "init_total_value")
	private BigDecimal initTotalValue = BigDecimal.ZERO;

	@Column(name = "account_value")
	private BigDecimal accountValue = BigDecimal.ZERO;

	@Column(name = "security_value")
	private BigDecimal securityValue = BigDecimal.ZERO;

	@Column(name = "position_value")
	private BigDecimal positionValue = BigDecimal.ZERO;

	@Column(name = "total_return")
	private BigDecimal totalReturnPercentage = BigDecimal.ZERO;

}


