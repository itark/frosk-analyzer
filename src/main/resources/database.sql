select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html


_____________________________________________________

drop table data_set_securities;
drop table data_set;
drop table security_price;
drop table security;
drop table trades;
drop table featured_strategy;


drop table chart_value;




SELECT * FROM SECURITY;
SELECT count(*) FROM SECURITY;

SELECT * FROM SECURITY
where name= 'SSAB-B.ST';

SELECT * FROM SECURITY_PRICE;

SELECT * FROM SECURITY_PRICE
where security_id = 310915
order by timestamp;

SELECT count(*) FROM SECURITY_PRICE;

SELECT * FROM featured_strategy;


select * from trades;

select * from data_set;
select * from data_set_securities;

  
        
        
        
        


_____________________________________________________


