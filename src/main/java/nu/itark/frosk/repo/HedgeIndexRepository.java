package nu.itark.frosk.repo;

import nu.itark.frosk.model.HedgeIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HedgeIndexRepository extends JpaRepository<HedgeIndex, Long> {

    List<HedgeIndex> findByDate(Date date);

    Optional<HedgeIndex> findTopByIndicatorOrderByDateDesc(@Param("indicator") String indicator);

    List<HedgeIndex> findByIndicator(String indicator);

    public interface RiskSummary {
        String getIndicator();

        Date getDate();

        Long getRiskCount();

        BigDecimal getPrice();
    }

    public interface RiskSummaryProjection {
        LocalDate getDayDate();

        String getIndicator();

        Long getTotalCount();

        Long getRiskyCount();

        Long getNonRiskyCount();

        Double getRiskyPercent();
    }


    @Query("SELECT h.indicator AS indicator, h.date AS date, COUNT(h) AS riskCount, MAX(h.price) AS price " +
            "FROM HedgeIndex h WHERE h.risk = TRUE GROUP BY h.indicator, h.date ORDER BY h.date")
    List<RiskSummary> summarizeRiskByDateDTO();

    // Java
    @Query(
            value = "SELECT CAST(date AS DATE) AS dayDate, " +
                    "indicator, " +
                    "COUNT(*) AS totalCount, " +
                    "SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS riskyCount, " +
                    "SUM(CASE WHEN risk = FALSE THEN 1 ELSE 0 END) AS nonRiskyCount, " +
                    "ROUND(100.0 * SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS riskyPercent " +
                    "FROM hedge_index " +
                    "GROUP BY CAST(date AS DATE), indicator " +
                    "ORDER BY 1, 2",
            nativeQuery = true)
    List<RiskSummaryProjection> summarizeRiskPerIndicatorAndDate();

    public interface RiskSummaryByDateProjection {
        LocalDate getDayDate();

        Long getTotalCount();

        Long getRiskyCount();

        Long getNonRiskyCount();

        Double getRiskyPercent();
    }

    @Query(
            value = "SELECT CAST(date AS DATE) AS dayDate, " +
                    "COUNT(*) AS totalCount, " +
                    "SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS riskyCount, " +
                    "SUM(CASE WHEN risk = FALSE THEN 1 ELSE 0 END) AS nonRiskyCount, " +
                    "ROUND(100.0 * SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS riskyPercent " +
                    "FROM hedge_index " +
                    "GROUP BY CAST(date AS DATE) " +
                    "ORDER BY 1",
            nativeQuery = true)
    List<RiskSummaryByDateProjection> summarizeRiskPerDate();


    public interface RiskCumulativeProjection {
        LocalDate getDayDate();

        Long getTotalCount();       // number of distinct indicators considered that day

        Long getRiskyCount();       // count of indicators with risk = true as of that day

        Long getNonRiskyCount();

        Double getRiskyPercent();
    }

    @Query(
            value =
                    "SELECT CAST(day_date AS DATE) AS dayDate, " +
                            "       COUNT(*) AS totalCount, " +
                            "       SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS riskyCount, " +
                            "       SUM(CASE WHEN risk = FALSE THEN 1 ELSE 0 END) AS nonRiskyCount, " +
                            "       ROUND(100.0 * SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0), 2) AS riskyPercent " +
                            "FROM ( " +
                            "  SELECT d.day_date, hi.indicator, hi.risk, " +
                            "         ROW_NUMBER() OVER (PARTITION BY d.day_date, hi.indicator ORDER BY hi.date DESC) rn " +
                            "  FROM (SELECT DISTINCT CAST(date AS DATE) AS day_date FROM hedge_index) d " +
                            "  JOIN hedge_index hi ON CAST(hi.date AS DATE) <= d.day_date " +
                            ") t " +
                            "WHERE rn = 1 " +
                            "GROUP BY CAST(day_date AS DATE) " +
                            "ORDER BY 1",
            nativeQuery = true)
    List<RiskCumulativeProjection> summarizeCumulativeRiskPerDate();


}
