package nu.itark.frosk.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
	
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "securities")
	private List<DataSet> datasets = new ArrayList<>();	
	
	protected Security() {
	}

	public Security(String name, String description, String database) {
		this.name = name;
		this.description = description;
		this.database = database;
	}
	
}


