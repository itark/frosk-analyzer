package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;
import nu.itark.frosk.bot.bot.dto.trade.OrderTypeDTO;
import nu.itark.frosk.model.dto.AccountTypeDTO;

import java.math.BigDecimal;
import java.util.Date;

import static jakarta.persistence.EnumType.STRING;

@Data
@Entity
@Table(name = "trading_account")
public class TradingAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date createDate;

	@Enumerated(STRING)
	@Column(name = "type")
	private AccountTypeDTO type;

	@Column(name = "init_total_value")
	private BigDecimal initTotalValue = BigDecimal.ZERO;

	@Column(name = "total_value")
	private BigDecimal totalValue = BigDecimal.ZERO;

	@Column(name = "security_value")
	private BigDecimal securityValue = BigDecimal.ZERO;

	@Column(name = "position_value")
	private BigDecimal positionValue = BigDecimal.ZERO;

	@Column(name = "total_return")
	private BigDecimal totalReturnPercentage = BigDecimal.ZERO;
}


