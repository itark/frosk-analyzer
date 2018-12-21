package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
@Entity
@Table(name = "strat_indicator_value", uniqueConstraints={@UniqueConstraint(columnNames={"indicator", "date", "featured_strategy_id"})})
public class StrategyIndicatorValue {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;

//	@NotNull
	@Column(name = "indicator")
	private String indicator;	
	
	@NotNull
	@Column(name = "value")
	private BigDecimal value;	
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="featured_strategy_id", nullable=false)
    private FeaturedStrategy featuredStrategy;

    protected StrategyIndicatorValue(){}
    
    public StrategyIndicatorValue(Date date, BigDecimal value, String indicator){
    	this.date = date;
    	this.value = value;
    	this.indicator = indicator;
    }
    

}
