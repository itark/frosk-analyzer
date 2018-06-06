package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import nu.itark.frosk.model.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long>{
	List<Customer> findByLastName(String lastName);
}
