package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "account_type")
public class AccountType {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date createDate;

	@Column(name = "type")
	private String type;

	@Column(name = "inherent_exitrule")
	private Boolean inherentExitRule;

	@OneToOne(mappedBy = "accountType")
	private TradingAccount tradingAccount;

}


