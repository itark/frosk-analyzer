package nu.itark.frosk.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration for the crypto DB: {@code intraday_bar.volume} BIGINT → DECIMAL(28,8).
 *
 * <p>Hibernate's {@code ddl-auto=update} never changes an existing column's type, so the
 * entity change from {@code Long} to {@code BigDecimal} needs this. The old column held
 * truncated integer volumes (0.32 stored as 0), which skewed the volume-weighted session VWAP.
 * Because already-stored bars cannot be corrected in place (existing timestamps are never
 * re-fetched), the 15m bars are deleted once, right after the type change; the next
 * {@code CryptoIntradayDataService} sync re-fetches the whole retention window with exact volume.
 *
 * <p>Idempotent: does nothing once the column is no longer BIGINT.
 */
@Component
@Profile("crypto")
@DependsOn("entityManagerFactory")
@Slf4j
public class IntradayBarVolumeMigration {

    private final JdbcTemplate jdbc;

    public IntradayBarVolumeMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void migrate() {
        try {
            String type = jdbc.query(
                    "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE UPPER(TABLE_NAME) = 'INTRADAY_BAR' AND UPPER(COLUMN_NAME) = 'VOLUME'",
                    rs -> rs.next() ? rs.getString(1) : null);
            if (type == null || !"BIGINT".equalsIgnoreCase(type)) {
                return;
            }
            jdbc.execute("ALTER TABLE intraday_bar ALTER COLUMN volume SET DATA TYPE DECIMAL(28,8)");
            int deleted = jdbc.update("DELETE FROM intraday_bar WHERE interval_code = '15m'");
            log.warn("IntradayBarVolumeMigration: volume BIGINT -> DECIMAL(28,8); deleted {} truncated 15m bars "
                    + "(re-fetched with exact volume on next sync)", deleted);
        } catch (Exception e) {
            log.error("IntradayBarVolumeMigration failed — volume stays truncated: {}", e.getMessage(), e);
        }
    }
}
