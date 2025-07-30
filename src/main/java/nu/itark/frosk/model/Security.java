package nu.itark.frosk.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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

	@Column(name = "active", columnDefinition="BOOLEAN DEFAULT true")
	private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "securities")
	private List<DataSet> datasets = new ArrayList<>();

	protected Security() {
	}


	public Security(String name, String description, String database, String quoteCurrency) {
		this.name = name;
		this.description = description;
		this.database = database;
		this.quoteCurrency = quoteCurrency;
		this.active = true;
	}

}


