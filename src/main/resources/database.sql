
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


SELECT * FROM SECURITY_PRICE
order by timestamp;


SELECT count(*) FROM SECURITY_PRICE
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




SELECT * FROM SECURITY;


truncate SECURITY;
_____________________________________________________
