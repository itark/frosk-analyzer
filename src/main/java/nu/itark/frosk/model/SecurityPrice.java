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
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
@Entity
@Table(name = "security_price", uniqueConstraints={@UniqueConstraint(columnNames={"security_id", "timestamp"})})
public class SecurityPrice implements Serializable {

	private static final long serialVersionUID = -3009157732242241606L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	//TODO gör PK av id och timestamp
	@NotNull
	@Column(name = "security_id")
	private Long securityId;

	@NotNull
	@Column(name = "timestamp")
	private Date timestamp;

	@NotNull
	@Column(name = "open")
	private BigDecimal open;

	@NotNull
	@Column(name = "high")
	private BigDecimal high;

	@NotNull
	@Column(name = "low")
	private BigDecimal low;	

	@NotNull
	@Column(name = "close")
	private BigDecimal close;	

	@NotNull
	@Column(name = "volume")
	private Long volume; 

	
	protected SecurityPrice() {
	}

	public SecurityPrice(long securityId, Date timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, Long volume) {
		this.securityId = securityId;
		this.timestamp = timestamp;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
		
	}

}
