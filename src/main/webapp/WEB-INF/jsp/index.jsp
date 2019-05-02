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

#clock-div {
  width: 100%;
  height: 98vh;
}
/*
 canvas { 
   display: block; 
   vertical-align: bottom; 
 } 
*/
#particles-js {
  position: absolute;
  width: 100%;
  height: 10%;
  background-color: #f2dede;
  background-image: url("");
  background-repeat: no-repeat;
  background-size: cover;
  background-position: 50% 50%; 
} 

</style>

<body>

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
	            <li><a href="ma">Moving Momentum</a></li>
	            <li><a href="rsi">Relative Strenght Index-2</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="all">All</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="echo">echo</a></li>
	            <li><a href="snake">snake</a></li>
	          </ul>
	        </li>
	      </ul>
	    </div>
	  </div>
	</nav>


	<div id="particles-js"></div>

	<header>
		 <div class="container-fluid">
			<div id="particles-js"></div>
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

		<div id="particles-js"></div>

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

<!--  
</div>
-->
<script src="https://cdn.jsdelivr.net/particles.js/2.0.0/particles.min.js"></script>
<script type="text/javascript">


particlesJS.load('particles-js', 'js/vg_particles.json', function() {
	  //console.log('callback - vg_particles.js config loaded');
});


</script>




<script>
 
    am4core.useTheme(am4themes_animated);

    // create chart
    var chart = am4core.create("clock-div", am4charts.GaugeChart);
    chart.exporting.menu = new am4core.ExportMenu();

    chart.startAngle = -90;
    chart.endAngle = 270;

    var axis = chart.xAxes.push(new am4charts.ValueAxis());
    axis.min = 0;
    axis.max = 12;
    axis.strictMinMax = true;

    axis.renderer.line.strokeWidth = 8;
    axis.renderer.line.strokeOpacity = 1;
    axis.renderer.minLabelPosition = 0.05; // hides 0 label
    axis.renderer.inside = true;
    axis.renderer.labels.template.radius = 35;
    axis.renderer.axisFills.template.disabled = true;
    axis.renderer.grid.template.disabled = true;
    axis.renderer.ticks.template.length = 12;
    axis.renderer.ticks.template.strokeOpacity = 1;

    // serves as a clock face fill
    var range = axis.axisRanges.create();
    range.startValue = 0;
    range.endValue = 12;
    range.grid.visible = false;
    range.tick.visible = false;
    range.label.visible = false;

    var axisFill = range.axisFill;
    axisFill.fillOpacity = 1;
    axisFill.disabled = false;
    axisFill.fill = am4core.color("#FFFFFF");

    // hands
    var hourHand = chart.hands.push(new am4charts.ClockHand());
    hourHand.radius = am4core.percent(60);
    hourHand.startWidth = 10;
    hourHand.endWidth = 10;
    hourHand.rotationDirection = "clockWise";
    hourHand.pin.radius = 8;
    hourHand.zIndex = 0;

    var minutesHand = chart.hands.push(new am4charts.ClockHand());
    minutesHand.rotationDirection = "clockWise";
    minutesHand.startWidth = 7;
    minutesHand.endWidth = 7;
    minutesHand.radius = am4core.percent(78);
    minutesHand.zIndex = 1;

    var secondsHand = chart.hands.push(new am4charts.ClockHand());
    secondsHand.fill = am4core.color("#DD0000");
    secondsHand.stroke = am4core.color("#DD0000");
    secondsHand.radius = am4core.percent(85);
    secondsHand.rotationDirection = "clockWise";
    secondsHand.zIndex = 2;
    secondsHand.startWidth = 1;

    updateHands();

    setInterval(() => {
      updateHands();
    }, 1000);

    function updateHands() {
      // get current date
      var date = new Date();
      var hours = date.getHours();
      var minutes = date.getMinutes();
      var seconds = date.getSeconds();

      // set hours
      hourHand.showValue(hours + minutes / 60, 0);
      // set minutes
      minutesHand.showValue(12 * (minutes + seconds / 60) / 60, 0);
      // set seconds
      secondsHand.showValue(12 * date.getSeconds() / 60, 300);
    }    
    
</script>

</body>

</html>
