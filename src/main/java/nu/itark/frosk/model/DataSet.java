package nu.itark.frosk.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
@Entity
@Table(name = "data_set")
public class DataSet implements Serializable {

	private static final long serialVersionUID = -3009157732242241606L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@NotNull
	@Size(max = 20)
	@Column(name = "name", unique=true)
	private String name;

	@NotNull
	@Size(max = 100)
	@Column(name = "description")
	private String description;

    @ManyToMany(fetch = FetchType.EAGER,cascade = CascadeType.ALL)
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


