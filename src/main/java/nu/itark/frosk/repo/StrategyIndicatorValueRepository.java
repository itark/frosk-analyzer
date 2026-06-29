package nu.itark.frosk.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import nu.itark.frosk.model.StrategyIndicatorValue;

@Repository
public interface StrategyIndicatorValueRepository extends JpaRepository<StrategyIndicatorValue, Long>{
	List<StrategyIndicatorValue> findByFeaturedStrategyId(Long featuredStrategyId);

	/**
	 * Bulk-delete by FK — avoids the JPQL OR-predicate chain that
	 * {@code deleteAllInBatch(list)} generates, which overflows Hibernate's
	 * ANTLR parser stack for large indicator-value sets (~11k rows/strategy).
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM StrategyIndicatorValue iv WHERE iv.featuredStrategy.id = :featuredStrategyId")
	void deleteByFeaturedStrategyId(Long featuredStrategyId);
}
