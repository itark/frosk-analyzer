<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!--  %@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%-->
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1 shrink-to-fit=no">
    <meta name="description" content="">
    <meta name="author" content="">
    <title>Evening Star</title>
</head>

<style>
#chart-div {
	width		: 100%;
	height		: 700px;
	font-size	: 11px;
}
header {
    padding: 1px 0 10px;
}
canvas {
  display: block;
  vertical-align: bottom;
}
#particles-js {
  position: absolute;
  width: 100%;
  height: 100%;
  background-color: #b61924;
  background-image: url("");
  background-repeat: no-repeat;
  background-size: cover;
  background-position: 50% 50%;
}
</style>

<body>

 <div id="particles-js"></div>

 <div id="wrapper">
	<nav class="navbar navbar-default">
	  <div class="container-fluid">
	    <div class="navbar-header">
	      <a class="navbar-brand" href="/frosk-analyzer" title="powered by Har-em Foundations">Evening Star</a>
	    </div>
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Strategies <span class="caret"></span></a>
	          <ul class="dropdown-menu">
	          	<li><a href="sma">Simple Moving Momentum</a></li>
	            <li><a href="ma">Moving Momentum</a></li>
	            <li><a href="rsi">Relative Strenght Index-2</a></li>
	            <li><a href="engulfing">Engulfing</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="all">All</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="loi">Limit Order Imbalance</a></li>
	          </ul>
	        </li>
	      </ul>
	    </div>
	  </div>
	</nav>

	<header>
		 <div class="container-fluid">
		   		<div class="row">
		          <div class="col-lg-12 col-md-12 text-center">
		            <h2>The trend is your friend</h2>
		            <p class="lead">Evening Star is your contemporary companion on the road of stock picking.</p>
		          </div>
		        </div>
		 </div>
	</header>
	<section>
      <div class="container-fluid">
        <div class="row">
           <div class="col-lg-12 col-md-12">
           	 <div class="col-12 dc-chart" id="clock-div"></div>
           </div>
        </div>
      </div>
   </section>
   
     <footer class="bg-primary text-white">
      <div class="container text-center">
        <p>Copyright &copy; Evening Star 2018</p>
      </div>
    </footer>   

 </div>

<script type="text/javascript">
    particlesJS.load('particles-js', 'js/vg_particles.json', function() {
          console.log('callback - vg_particles.js config loaded');
    });
</script>

</body>

</html>
