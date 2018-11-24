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
	height		: 750px;
	font-size	: 11px;
}

header {
    padding: 15px 0 10px;
}

</style>

<body>

 <div id="wrapper">
 
	<nav class="navbar navbar-default">
	  <div class="container-fluid">
	    
	    <div class="navbar-header">
	      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
	        <span class="sr-only">Toggle navigation</span>
	        <span class="icon-bar"></span>
	        <span class="icon-bar"></span>
	        <span class="icon-bar"></span>
	      </button>
	      <a class="navbar-brand" href="/frosk-analyzer" title="powered by Har-em Foundations">Evening Star</a>
	    </div>
	   
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <!--  li class="active"><a href="#">�versikt <span class="sr-only">(current)</span></a></li-->
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Strategies <span class="caret"></span></a>
	          <ul class="dropdown-menu">
	            <li><a href="ma">Moving Momentum</a></li>
	            <li><a href="rsi">Relative Strenght Index-2</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="all">All</a></li>
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
            <h2>All</h2>
			<pre class="code">
[Moving Average]
[RSI-2]
[Global Extrema]
[CCI Correction]
			</pre>
          </div>
        </div> 
 
        <div class="row">
            <div class="col-lg-4 col-md-4">
                <div class="panel panel-default">
                    <div class="panel-heading">
  						<i class="fa fa-table fa-fw"></i>
                        <div class="pull-right">
                            <div class="btn-group">
                                <button id="dataset" type="button" class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
                                	Dataset
                                    <span class="caret"></span>
                                </button>
                                <ul class="dropdown-menu pull-right" role="menu">
                                    <li><a href ="#" onclick="renderTable('INDEX');">INDEX</a></li>
                                    <li><a href="#" onclick="renderTable('OMX30');">OMX30</a></li>
                                    <li><a href ="#" onclick="renderTable('OSCAR');">OSCAR</a></li>
                                </ul>
                            </div>
                        </div>
					</div>
                   <div class="panel-body">
                     <table class="table table-striped table-bordered table-hover" id="featuredStrategies">
					  <thead>
				            <tr>
				                <th>Strategy</th>
				                <th>Security</th>
				                <th>Profit%</th>
				                <th>Trades</th>
				                <th>LatestTrade</th>				                
				                <th>Period</th>
								<th>Ticks</th>
				                <th>AverageProfit</th>
				                <th>Ratio</th>
				                <th>MaxDD</th>
				            </tr>
				        </thead>
				        <tfoot>
				            <tr>
				                <th>Strategy</th>
				                <th>Security</th>
				                <th>Profit%</th>
				                <th>Trades</th>
				                <th>LatestTrade</th>
				                <th>Period</th>
								<th>Ticks</th>
				                <th>AverageProfit</th>
				                <th>Ratio</th>
				                <th>MaxDD</th>
				            </tr>
				        </tfoot>
                     </table>
                   </div>
                </div>
           </div>
 
           <div class="col-lg-8 col-md-8">
                 <div class="panel panel-default">
                        <div class="panel-heading">
                            <i class="fa fa-bar-chart-o fa-fw"></i>
                            <div class="pull-right">
                                <div class="btn-group">
                                    <button type="button" class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
                                        Chart type
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu pull-right" role="menu">
                                        <li><a href="#" onclick="renderChartOHLC();">Candle</a>
                                        </li>
                                        <li><a href="#" onclick="renderChartLine();">Line</a>
                                        <li><a href="#" onclick="renderChartLineWithAddons();">Line addons</a>
                                        </li>                                        
                                    </ul>
                                </div>
                            </div>
                        </div>
                        <!-- /.panel-heading -->
	   					<div class="panel-body">
			           		<div class="col-12 dc-chart" id="chart-div"></div>
	  					</div>
                        <!-- /.panel-body -->
                    </div>
                    <!-- /.panel -->
           </div>
         </div>
 		</div>
     </header>

   
     <footer class="bg-primary text-white">
      <div class="container text-center">
        <p>Copyright &copy; Evening Star 2018</p>
      </div>
    </footer>   

</div>

 <script>

 	strategy="ALL";

</script>

</body>

</html>
