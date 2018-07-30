select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html



--Prices
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

CREATE SEQUENCE security_price_seq;


SELECT * FROM SECURITY_PRICE
order by timestamp;


SELECT count(*) FROM SECURITY_PRICE
where name = 'GOOG';

SELECT DISTINCT name FROM SECURITY_PRICE;


SELECT MAX(timestamp) FROM SECURITY_PRICE
where name = 'GOOG';



truncate SECURITY_PRICE;

_____________________________________________________


--Security
drop table security;

CREATE TABLE security(
    id BIGINT PRIMARY KEY  NOT NULL,
    name VARCHAR(20) UNIQUE NOT NULL,
	description VARCHAR(50),
	database VARCHAR(10) NOT NULL
);


CREATE SEQUENCE security_seq;

SELECT * FROM SECURITY;


truncate SECURITY;
_____________________________________________________





select * from pg_sequence

select * from pg_database


select * from hibernate_sequence
