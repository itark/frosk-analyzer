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
@Table(name = "security_price", uniqueConstraints={@UniqueConstraint(columnNames={"name", "timestamp"})})
public class SecurityPrice implements Serializable {

	private static final long serialVersionUID = -3009157732242241606L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "name")
	private String name;
	@Column(name = "timestamp", unique=true)
	private Date timestamp;
	@Column(name = "open")
	private BigDecimal open;
	@Column(name = "high")
	private BigDecimal high;
	@Column(name = "low")
	private BigDecimal low;	
	@Column(name = "close")
	private BigDecimal close;	
	@Column(name = "volume")
	private Long volume; 

	
	protected SecurityPrice() {
	}

	public SecurityPrice(String name, Date timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {
		this.name = name;
		this.timestamp = timestamp;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
		
	}

}
