package nu.itark.frosk.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Data
@Entity
@Table(name = "security")
public class Security {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "name", unique=true)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "database")
	private String database;

	@Column(name = "quote_currency")
	private String quoteCurrency;

	@Column(name = "yoy_growth")
	private Double yoyGrowth;

	@Column(name = "peg_ratio")
	private Double pegRatio;

	@Column(name = "beta")
	private Double beta;

	@Column(name = "trailing_eps")
	private Double trailingEps;

	@Column(name = "forward_eps")
	private Double forwardEps;

	@Column(name = "trailing_pe")
	private Double trailingPe;

	@Column(name = "forward_pe")
	private Double forwardPe;

	@Column(name = "enterprise_value")
	private Long enterpriseValue;

	@Column(name = "active", columnDefinition="BOOLEAN DEFAULT true")
	private boolean active = true;

	@PrePersist
	@PreUpdate
	private void syncActiveWithEnterpriseValue() {
		this.active = (this.enterpriseValue != null && this.enterpriseValue > 500000000);
	}

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "securities")
	private List<DataSet> datasets = new ArrayList<>();

	protected Security() {
	}

	public Security(String name, String description, String database, String quoteCurrency) {
		this.name = name;
		this.description = description;
		this.database = database;
		this.quoteCurrency = quoteCurrency;
	}

}


