select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html


_____________________________________________________

drop table security;

CREATE TABLE security(
    id BIGINT NOT NULL,
    name VARCHAR(20) UNIQUE NOT NULL,
	description VARCHAR(50),
	database VARCHAR(10) NOT NULL,
	PRIMARY KEY (id)
);

SELECT * FROM SECURITY;

--truncate SECURITY;
_____________________________________________________

drop table security_price;

CREATE TABLE security_price(
    id BIGINT  NOT NULL,
    security_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
	open double precision NOT NULL,
	high double precision NOT NULL,
	low  double precision NOT NULL,
	close  double precision NOT NULL,
	volume BIGINT NOT NULL,
	PRIMARY KEY (id, security_id, timestamp ),
	CONSTRAINT security_price_security_id_fkey FOREIGN KEY (security_id)
      REFERENCES security (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION	
);


SELECT count(*) FROM SECURITY_PRICE;

SELECT s.name FROM SECURITY_PRICE sp, SECURITY s
WHERE sp.security_id = s.id
AND s.name = 'SAND.ST';

SELECT * FROM SECURITY_PRICE sp
WHERE sp.security_id = 122128;

select security_id from SECURITY_PRICE;

--truncate SECURITY_PRICE;

_____________________________________________________

--Featured Strategy
drop table featured_strategy;

CREATE TABLE featured_strategy(
    id BIGINT  NOT NULL,
    name VARCHAR(50) NOT NULL,
    security_id BIGINT NOT NULL,
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
	PRIMARY KEY (id, name, security_id),
	CONSTRAINT featured_strategy_security_id_fkey FOREIGN KEY (security_id)
      REFERENCES security (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);

SELECT * FROM featured_strategy;

truncate featured_strategy;

_____________________________________________________

drop table data_set;

CREATE TABLE data_set(
    id BIGINT  NOT NULL,
    name VARCHAR(20) UNIQUE NOT NULL,
    description VARCHAR(100) NOT NULL,
	PRIMARY KEY (id)
);

--truncate data_set;

_____________________________________________________


drop table data_set_securities;

CREATE TABLE data_set_securities(
    data_set_id BIGINT NOT NULL,
    security_id BIGINT NOT NULL
	PRIMARY KEY (data_set_id,  security_id),
  	CONSTRAINT data_set_securities_data_set_id_fkey FOREIGN KEY (data_set_id)
      REFERENCES data_set (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT data_set_securities_security_id_fkey FOREIGN KEY (security_id)
      REFERENCES security (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION	
	
);

--truncate data_set_group;

_____________________________________________________


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




SELECT * from FEATURED_STRATEGY;
SELECT count(*) from FEATURED_STRATEGY;

SELECT latest_trade from FEATURED_STRATEGY;


SELECT * FROM CHART_VALUE

SELECT * FROM SECURITY_PRICE
order by timestamp;


SELECT name FROM SECURITY_PRICE
where name = 'SAND.ST';

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




