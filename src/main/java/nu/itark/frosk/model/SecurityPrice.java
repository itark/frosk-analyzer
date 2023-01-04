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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "security_price", uniqueConstraints={@UniqueConstraint(columnNames={"security_id", "timestamp"})})
public class SecurityPrice implements Serializable {

	private static final long serialVersionUID = -3009157732242241606L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "security_id")
	private Long securityId;

	@Column(name = "timestamp")
	private Date timestamp;

	@Column(name = "open",precision=12, scale=6)
	private BigDecimal open;

	@Column(name = "high",precision=12, scale=6)
	private BigDecimal high;

	@Column(name = "low",precision=12, scale=6)
	private BigDecimal low;

	@Column(name = "close",precision=12, scale=6)
	private BigDecimal close;	

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
