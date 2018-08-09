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

truncate SECURITY;
_____________________________________________________



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


truncate SECURITY_PRICE;


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


