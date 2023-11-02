package nu.itark.frosk.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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


