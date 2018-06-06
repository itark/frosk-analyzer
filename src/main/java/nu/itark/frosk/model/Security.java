package nu.itark.frosk.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(name = "security", uniqueConstraints={@UniqueConstraint(columnNames={"name"})})
public class Security implements Serializable {
	private static final long serialVersionUID = -3009157732242241606L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(name = "name", unique=true)
	private String name;
	@Column(name = "description")
	private String description;
	@Column(name = "database")
	private String database;
	
	protected Security() {
	}

	public Security(String name, String description, String database) {
		this.name = name;
		this.description = description;
		this.database = database;
	}
	
}


