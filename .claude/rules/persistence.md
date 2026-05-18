---
globs: src/main/java/**/model/**,src/main/java/**/repo/**
---

# Persistence Rules

- Entities use Jakarta Persistence (`jakarta.persistence.*`), Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`), and `@Entity`/`@Table`.
- Use `GenerationType.AUTO` for `@Id` generation (H2 in dev, compatible with Postgres).
- Repositories extend `JpaRepository<Entity, Long>` and are annotated `@Repository`.
- Prefer derived query methods (`findByNameAndOpen`) over `@Query` unless the query is complex.
- `FeaturedStrategy` is keyed by (strategy name, security name). It carries `lastRunDate` for same-day idempotency — do not remove or bypass this field.
- `StrategyTrade.date` uses `@Temporal(TemporalType.TIMESTAMP)` to preserve hour/minute for intraday trades.
- `IntradayBar` stores timestamps as epoch seconds (`long`), not `Date` or `Instant`, to avoid timezone ambiguity across DST transitions. The unique constraint `(security_id, bar_timestamp, interval_code)` prevents duplicate bars.
- `IntradaySignal` DB columns are named `ema9`, `ema21`, `rsi7` for backwards compatibility even though the strategy uses EMA5/EMA13/RSI5.
- When adding new entity fields, add the corresponding column via Hibernate auto-DDL (`spring.jpa.hibernate.ddl-auto=update`). No manual migration scripts are used.
