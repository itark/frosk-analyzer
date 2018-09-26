select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html


_____________________________________________________

drop table security;
drop table security_price;
drop table featured_strategy;
drop table data_set;
drop table data_set_securities;
drop table chart_value;




SELECT * FROM SECURITY;

SELECT count(*) FROM SECURITY_PRICE;

SELECT s.name FROM SECURITY_PRICE sp, SECURITY s
WHERE sp.security_id = s.id
AND s.name = 'SAND.ST';

SELECT * FROM SECURITY_PRICE sp
WHERE sp.security_id = 122128;

select security_id from SECURITY_PRICE;



SELECT * FROM featured_strategy;


select * from data_set;
select * from data_set_securities;
_____________________________________________________


