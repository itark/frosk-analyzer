package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;
import nu.itark.frosk.model.dto.AccountTypeDTO;

import java.math.BigDecimal;
import java.util.Date;

import static jakarta.persistence.EnumType.STRING;

@Data
@Entity
@Table(name = "account_type")
public class AccountType {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date createDate;

	@Enumerated(STRING)
	@Column(name = "type")
	private AccountTypeDTO type;

}


