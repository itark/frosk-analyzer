package nu.itark.frosk.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Data
@Entity
@Table(name = "strategy_performance", uniqueConstraints={@UniqueConstraint(columnNames={"date", "security_name", "best_strategy"})})
public class StrategyPerformance {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;

	@Column(name = "security_name")
	private String securityName;

	@Column(name = "best_strategy")
	private String bestStrategy;

	@Column(name = "total_profit_loss")
	private BigDecimal totalProfitLoss;


}


