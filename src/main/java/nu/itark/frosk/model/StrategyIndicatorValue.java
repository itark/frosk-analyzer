package nu.itark.frosk.model;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "strat_indicator_value", uniqueConstraints={@UniqueConstraint(columnNames={"indicator", "date", "featured_strategy_id"})})
public class StrategyIndicatorValue {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;

	@Column(name = "indicator")
	private String indicator;	
	
	@Column(name = "value_", precision=12, scale=6)  //value invalid for spring boot 2.7,hence the _
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
