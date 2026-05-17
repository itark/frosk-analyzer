# Useful H2 Queries

## H2 Console

Available at `/h2-console` when the app is running. Connection URL is in `application.properties`.

## Portfolio & Strategy Queries

```sql
-- Current portfolio: open positions with quality metrics
SELECT fs.security_name, fs.name strategy, fs.sqn, fs.prof_trade_ratio win_rate,
       fs.total_gross_return, t.price entry_price, t.date entry_date
FROM featured_strategy fs
JOIN strategy_trade t ON t.featured_strategy_id = fs.id
WHERE fs.open = true
  AND fs.name != 'HedgeIndexStrategy'
  AND t.type = 'BUY'
  AND t.date = (SELECT MAX(t2.date) FROM strategy_trade t2
                WHERE t2.featured_strategy_id = fs.id AND t2.type = 'BUY')
ORDER BY fs.sqn DESC;

-- Today's new BUY signals
SELECT fs.security_name, fs.name strategy, fs.sqn, t.price, t.date
FROM featured_strategy fs
JOIN strategy_trade t ON t.featured_strategy_id = fs.id
WHERE fs.open = true AND t.type = 'BUY'
  AND CAST(t.date AS DATE) = CURRENT_DATE
ORDER BY fs.sqn DESC;

-- Current HedgeIndex score (most recent date)
-- Note: HEDGE_INDEX stores one row per indicator per date (columns: id, category, date, indicator, price, risk, rule_desc)
-- Score = count of RISK=TRUE rows for the latest date
SELECT date, SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS score, COUNT(*) AS total_indicators
FROM hedge_index
WHERE date = (SELECT MAX(date) FROM hedge_index)
GROUP BY date;

-- HedgeIndex history (last 30 days)
SELECT date, SUM(CASE WHEN risk = TRUE THEN 1 ELSE 0 END) AS score, COUNT(*) AS total_indicators
FROM hedge_index
WHERE date >= DATEADD('DAY', -30, CURRENT_DATE)
GROUP BY date
ORDER BY date DESC;
```

## Intraday Queries

```sql
-- Latest 20 intraday signals (BUY and SELL)
SELECT signal_type, ticker,
       DATEADD('SECOND', signal_timestamp, DATE '1970-01-01') AS bar_time,
       close_price, ema9, ema21, rsi7
FROM intraday_signal
ORDER BY signal_timestamp DESC
LIMIT 20;

-- Only today's BUY signals
SELECT signal_type, ticker,
       DATEADD('SECOND', signal_timestamp, DATE '1970-01-01') AS bar_time,
       close_price, ema9, ema21, rsi7
FROM intraday_signal
WHERE signal_type = 'BUY'
  AND signal_timestamp >= UNIX_TIMESTAMP(CURRENT_DATE)
ORDER BY signal_timestamp DESC;

-- Bar count currently in the rolling window
SELECT COUNT(*) AS bar_count,
       MIN(DATEADD('SECOND', bar_timestamp, DATE '1970-01-01')) AS oldest_bar,
       MAX(DATEADD('SECOND', bar_timestamp, DATE '1970-01-01')) AS newest_bar
FROM intraday_bar;
```
