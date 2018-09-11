package nu.itark.frosk.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "chart_value", uniqueConstraints={@UniqueConstraint(columnNames={"security"})})
public class ChartValue implements Serializable {
	private static final long serialVersionUID = -3009157732242241606L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "security", unique=true)
	private String security;
	@Column(name = "strategy", unique=true)
	private String strategy;
	@Column(name = "timestamp", unique=true)
	private Date timestamp;
	@Column(name = "value1")
	private BigDecimal value1;
	@Column(name = "value2")
	private BigDecimal value2;
	
	protected ChartValue() {
	}

	public ChartValue(String security, String strategy, Date timestamp, BigDecimal value1, BigDecimal value2) {
		this.security = security;
		this.strategy = strategy;
		this.timestamp = timestamp;
		this.value1 = value1;
		this.value2 = value2;
	}
	
}


