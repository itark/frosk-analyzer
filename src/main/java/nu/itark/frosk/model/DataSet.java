package nu.itark.frosk.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "data_set")
public class DataSet  {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "name", unique=true)
	private String name;

	@Column(name = "description")
	private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "data_set_securities",
            joinColumns = { @JoinColumn(name = "data_set_id") },
            inverseJoinColumns = { @JoinColumn(name = "security_id") })
    private List<Security> securities = new ArrayList<>();	
	
	
	public DataSet() {
	}

	public DataSet(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
}


