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

import lombok.Data;

@Data
@Entity
@Table(name = "strategy_trade", uniqueConstraints={@UniqueConstraint(columnNames={"date", "type", "featured_strategy_id"})})
public class StrategyTrade {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	private Date date;
	
	@Column(name = "price")
	private BigDecimal price;	
	
	@Column(name = "type")
	private String type;
	
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="featured_strategy_id", nullable=false)
    private FeaturedStrategy featuredStrategy;

    protected StrategyTrade(){}
    
    public StrategyTrade(Date date, String type, BigDecimal price){
    	this.date = date;
    	this.price = price;
    	this.type = type;
    }
    

}
