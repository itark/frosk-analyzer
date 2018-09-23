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

select * from data_set;
select * from data_set_securities;
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


select * from data_set_securities;


select * from data_set;


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




org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role: nu.itark.frosk.model.Security.datasets, could not initialize proxy - no Session
	at org.hibernate.collection.internal.AbstractPersistentCollection.throwLazyInitializationException(AbstractPersistentCollection.java:587)
	at org.hibernate.collection.internal.AbstractPersistentCollection.withTemporarySessionIfNeeded(AbstractPersistentCollection.java:204)
	at org.hibernate.collection.internal.AbstractPersistentCollection.initialize(AbstractPersistentCollection.java:566)
	at org.hibernate.collection.internal.AbstractPersistentCollection.read(AbstractPersistentCollection.java:135)
	at org.hibernate.collection.internal.PersistentSet.toString(PersistentSet.java:299)
	at java.lang.String.valueOf(String.java:2994)
	at java.lang.StringBuilder.append(StringBuilder.java:131)
	at nu.itark.frosk.model.Security.toString(Security.java:21)
	at java.lang.String.valueOf(String.java:2994)
	at java.lang.StringBuilder.append(StringBuilder.java:131)
	at nu.itark.frosk.repo.TestJSecurityRepository.testFindByName(TestJSecurityRepository.java:38)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks.evaluate(RunBeforeTestMethodCallbacks.java:75)
	at org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks.evaluate(RunAfterTestMethodCallbacks.java:86)
	at org.springframework.test.context.junit4.statements.SpringRepeat.evaluate(SpringRepeat.java:84)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.runChild(SpringJUnit4ClassRunner.java:252)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.runChild(SpringJUnit4ClassRunner.java:94)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
	at org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks.evaluate(RunBeforeTestClassCallbacks.java:61)
	at org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks.evaluate(RunAfterTestClassCallbacks.java:70)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
	at org.springframework.test.context.junit4.SpringJUnit4ClassRunner.run(SpringJUnit4ClassRunner.java:191)
	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:86)
	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:459)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:678)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:382)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:192)

