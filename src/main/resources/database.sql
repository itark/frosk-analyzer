select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html

--Security
drop table security;

CREATE TABLE security(
    id BIGINT PRIMARY KEY  NOT NULL,
    name VARCHAR(20) UNIQUE NOT NULL,
	description VARCHAR(50),
	database VARCHAR(10) NOT NULL
);

SELECT * FROM SECURITY;

--truncate SECURITY;
_____________________________________________________

--Security Prices
drop table security_price;

CREATE TABLE security_price(
    id BIGINT  NOT NULL,
    name VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
	open double precision NOT NULL,
	high double precision NOT NULL,
	low  double precision NOT NULL,
	close  double precision NOT NULL,
	volume BIGINT NOT NULL,
	 PRIMARY KEY (id, name, timestamp )
);


--truncate SECURITY_PRICE;

_____________________________________________________

--Security Prices
drop table chart_value;

CREATE TABLE chart_value(
    id BIGINT  NOT NULL,
    security VARCHAR(20) NOT NULL,
    strategy VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
	value1 double precision NOT NULL,
	value2 double precision NOT NULL,
	PRIMARY KEY (id, security, strategy, timestamp )
);


--truncate chart_value;

_____________________________________________________

--Featured Strategy
drop table featured_strategy;

CREATE TABLE featured_strategy(
    id BIGINT  NOT NULL,
    name VARCHAR(50) NOT NULL,
    security VARCHAR(20) NOT NULL,
    latest_trade TIMESTAMP NOT NULL,
    total_profit double precision,
    avg_tick_profit double precision,
	trades BIGINT NOT NULL,
	prof_trade_ratio  double precision,
	max_dd  double precision,
	rew_risk_ratio double precision,
	transaction_cost  double precision NOT NULL,
	buy_hold  double precision NOT NULL,
	buy_vs_hold  double precision NOT NULL,
	period VARCHAR(50) NOT NULL,
	ticks BIGINT NOT NULL,
	 PRIMARY KEY (id, name,security)
);


--truncate featured_strategy;

_____________________________________________________


SELECT * from FEATURED_STRATEGY;
SELECT count(*) from FEATURED_STRATEGY;

SELECT latest_trade from FEATURED_STRATEGY;


SELECT * FROM CHART_VALUE

SELECT * FROM SECURITY_PRICE
order by timestamp;


SELECT count(*) FROM SECURITY_PRICE
where name = 'GOOG';

SELECT count(*) FROM SECURITY_PRICE
where name = '^OMXS30';


SELECT max(timestamp) FROM SECURITY_PRICE
where name = 'GOOG';

delete from security_price
where name = 'GOOG'
and timestamp > '2018-08-03'



SELECT DISTINCT name FROM SECURITY_PRICE;


SELECT MAX(timestamp) FROM SECURITY_PRICE
where name = 'GOOG';

_____________________________________________________




