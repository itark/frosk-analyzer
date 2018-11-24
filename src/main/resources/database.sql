select version();

--em2
https://www.postgresql.org/docs/9.6/static/index.html


_____________________________________________________

drop table data_set_securities;
drop table data_set;
drop table security_price;
drop table security;
drop table strategy_trade;
drop table strat_indicator_value;
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

SELECT * FROM featured_strategy
--where id = 465678;
where security_name LIKE '%ABB%';


select * from strategy_trade
--delete from strategy_trade
where featured_strategy_id = 465673;

select * from strat_indicator_value
select count(*) from strat_indicator_value;
where featured_strategy_id = 465673
order by date;

select * from data_set;
select * from data_set_securities;

  
        
        
        
        


_____________________________________________________


