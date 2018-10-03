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
SELECT count(*) FROM SECURITY_PRICE;

SELECT * FROM featured_strategy;


select * from data_set;
select * from data_set_securities;



        <div class="row">
          <div class="col-lg-12 col-md-12 text-center">
            <h2>The Moving Momentum, a.k.a Moving Average</h2>
            <p>Moving averages are trend-following indicators that lag price. This strategy employs two moving averages to define the trading bias. 
            The bias is bullish when the shorter-moving average moves above the longer moving average. The bias is bearish when the shorter-moving average moves below the longer moving average.</p>
          </div>
        </div>

        
        <div class="row">
          <div class="col-lg-12 col-md-12 text-center">
            <h2>The Relative Strength Index - 2</h2>
            <p>Connors suggests looking for buying opportunities when 2-period RSI moves below 10, which is considered deeply oversold. Conversely, traders can look for short-selling opportunities when 2-period RSI moves above 90..</p>
          </div>
        </div>    
        
         <div class="row">
          <div class="col-lg-12 col-md-12 text-center">
            <h2>All</h2>
			<pre class="code">
[Moving Average]
[RSI-2]
			</pre>
          </div>
        </div>        
        
        
        
        


_____________________________________________________


