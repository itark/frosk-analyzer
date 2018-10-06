package nu.itark.frosk.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import nu.itark.frosk.model.Trades;

public interface TradesRepository extends JpaRepository<Trades, Long>{
	
}
