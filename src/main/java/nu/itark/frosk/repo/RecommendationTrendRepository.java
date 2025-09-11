package nu.itark.frosk.repo;

import nu.itark.frosk.model.RecommendationTrend;
import nu.itark.frosk.model.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RecommendationTrend
 */
@Repository
public interface RecommendationTrendRepository extends JpaRepository<RecommendationTrend, Long> {

    /**
     * Find the latest current period trend for a security
     */
    @Query("SELECT rt FROM RecommendationTrend rt WHERE rt.security = :security AND rt.period = '0m' ")
    Optional<RecommendationTrend> findLatestCurrentTrendBySecurity(@Param("security") Security security);

    /**
     * Find the latest current period trend by security name
     */
    @Query("SELECT rt FROM RecommendationTrend rt WHERE rt.security.name = :securityName AND rt.period = '0m' ")
    Optional<RecommendationTrend> findLatestCurrentTrendBySecurityName(@Param("securityName") String securityName);

    @Transactional
    @Modifying
    @Query("DELETE FROM RecommendationTrend rt WHERE rt.security = :security")
    void deleteBySecurity(@Param("security") Security security);

    /**
     * Find all current period trends for a security (for history)
     */
    List<RecommendationTrend> findBySecurityAndPeriod(Security security, String period);

    /**
     * Find all trends from a specific API call (same processed time)
     */
    List<RecommendationTrend> findBySecurityOrderByPeriod(Security security);

    /**
     * Get distinct securities that have recommendation data
     */
    @Query("SELECT DISTINCT rt.security FROM RecommendationTrend rt")
    List<Security> findSecuritiesWithRecommendations();
}
