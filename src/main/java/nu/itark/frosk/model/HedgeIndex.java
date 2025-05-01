package nu.itark.frosk.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "hedge_index")
public class HedgeIndex {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;

	@Column(name = "category")
	private String category;

	@Column(name = "indicator")
	private String indicator;

	@Column(name = "rule_desc")
	private String ruleDesc;

	@Column(name = "risk")
	private Boolean risk;

	@Column(name = "price")
	private BigDecimal price;

}


