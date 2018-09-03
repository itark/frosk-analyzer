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

    <!-- DataTables CSS -->
    <link href="webjars/datatables/css/dataTables.bootstrap.css" rel="stylesheet">
 
    <!-- DataTables Responsive CSS -->
    <link href="https://cdn.datatables.net/responsive/1.0.6/css/dataTables.responsive.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/select/1.2.3/css/select.dataTables.min.css" rel="stylesheet">

 	<link rel="stylesheet" href="https://www.amcharts.com/lib/3/plugins/export/export.css" type="text/css" media="all" />
 	<link href="https://cdn.jsdelivr.net/npm/startbootstrap-scrolling-nav@4.1.1/css/scrolling-nav.css" rel="stylesheet">
    

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
	        <!--  li class="active"><a href="#">Översikt <span class="sr-only">(current)</span></a></li-->
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
            <h2>The Moving Momentum, a.k.a Moving Average</h2>
            <p>Moving averages are trend-following indicators that lag price. This strategy employs two moving averages to define the trading bias. 
            The bias is bullish when the shorter-moving average moves above the longer moving average. The bias is bearish when the shorter-moving average moves below the longer moving average.</p>
          </div>
        </div>
 
        <div class="row">
            <div class="col-lg-3 col-md-3">
                <div class="panel panel-default">
                    <div class="panel-heading">
                     	OMX30
	                   <div class="panel-body">
	                     <table class="table table-striped table-bordered table-hover" id="featuredStrategies">
						  <thead>
					            <tr>
					                <th>Security</th>
					                <th>Profit %</th>
					                <th>LatestTrade</th>				                
					                <th>Period</th>
									<th>Ticks</th>
					                <th>AverageProfit</th>
					                <th>Trades</th>
					                <th>Ratio</th>
					                <th>MaxDD</th>
					            </tr>
					        </thead>
					        <tfoot>
					            <tr>
					                <th>Security</th>
					                <th>Profit %</th>
					                <th>LatestTrade</th>
					                <th>Period</th>
									<th>Ticks</th>
					                <th>AverageProfit</th>
					                <th>Trades</th>
					                <th>Ratio</th>
					                <th>MaxDD</th>
					            </tr>
					        </tfoot>
	                     </table>
	                   </div>
                    </div>
                </div>
            </div>
 
           <div class="col-lg-9 col-md-9">
                 <div class="panel panel-default">
                        <div class="panel-heading">
                            <i class="fa fa-bar-chart-o fa-fw"></i>
                            <div class="pull-right">
                                <div class="btn-group">
                                    <button type="button" class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
                                        Actions
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu pull-right" role="menu">
                                        <li><a href="#">Action</a>
                                        </li>
                                        <li><a href="#" onclick="renderChart2('BOL.ST');">Line</a>
                                        </li>
                                        <li><a href="#">Something else here</a>
                                        </li>
                                        <li class="divider"></li>
                                        <li><a href="#">Separated link</a>
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


    <!-- DataTables JavaScript -->
    <script src="webjars/datatables/js/jquery.dataTables.min.js"></script>
    <script src="webjars/datatables/js/dataTables.bootstrap.min.js"></script> 
    <script src="https://cdn.jsdelivr.net/webjars/org.webjars.bower/datatables.net-responsive/2.1.1/js/dataTables.responsive.js"></script>
    <script src="https://cdn.datatables.net/select/1.2.3/js/dataTables.select.min.js"></script>
	<script src="https://www.amcharts.com/lib/3/amcharts.js"></script>
	<script src="https://www.amcharts.com/lib/3/serial.js"></script>
	<script src="https://www.amcharts.com/lib/3/amstock.js"></script>
	<script src="https://www.amcharts.com/lib/3/plugins/dataloader/dataloader.min.js"></script>
	<script src="https://www.amcharts.com/lib/3/plugins/export/export.min.js"></script>
	<script src="https://www.amcharts.com/lib/3/themes/light.js"></script>
	<script src="https://cdn.jsdelivr.net/npm/startbootstrap-scrolling-nav@4.1.1/js/scrolling-nav.js"></script>
    
	<script src="https://www.amcharts.com/lib/4/core.js"></script>
	<script src="https://www.amcharts.com/lib/4/charts.js"></script>
	<script src="https://www.amcharts.com/lib/4/themes/animated.js"></script>  
    

    <!-- Page-Level Demo Scripts - Tables - Use for reference -->
 <script>
 	var selectedSecurity;
 
 
    $(document).ready(function() {
    	var events = $('#events');
        var featStratTable = $('#featuredStrategies').DataTable({
        	responsive: true,
        	select: true,
        	"sAjaxSource": "featuredStrategies?strategy=MovingMomentumStrategy",
			"sAjaxDataProp": "",
			"order": [[ 1, "desc" ]],
			"aoColumns": [
				  { "mData": "security"},
			      { "mData": "totalProfit"},
				  { "mData": "latestTradeDate" },
				  { "mData": "periodDescription"},
			      { "mData": "numberOfTicks" },
				  { "mData": "averageTickProfit" },
				  { "mData": "numberofTrades" },
				  { "mData": "profitableTradesRatio" },
				  { "mData": "maxDD" }
			]       
        
        });
   
        featStratTable
    		.on( 'select', function ( e, dt, type, indexes ) {
            	var rowData = featStratTable.rows( indexes ).data().toArray();
            	//var name = featStratTable.rows( indexes ).data().pluck( 'name' );
//             	var name = "MovingMomentumStrategy";
            	var security = featStratTable.rows( indexes ).data().pluck( 'security' );
            	selectedSecurity = security[0];
            	renderChart(security[0]);
            	
        }) ; 
        
    });
    

    function renderChart(security) {
    	console.log('about to render chart on strategyName=MovingMomentumStrategy and security='+security);
    	
     	var dailyPricesUrl = "dailyPrices?security="+security;
     	var tradesUrl = "trades?security="+security+"&strategy=MovingMomentumStrategy";
     	var indicatorValueUrl = "rsiValues?security="+security+"&strategy=MovingMomentumStrategy";

     	console.log("dailyPricesUrl",dailyPricesUrl);
     	console.log("tradesUrl",tradesUrl);
    	console.log("indicatorValueUrl",indicatorValueUrl);
	 
    	am4core.useTheme(am4themes_animated);

    	var chart = am4core.create("chart-div", am4charts.XYChart);
    	chart.dataSource.url = dailyPricesUrl;

    	chart.paddingRight = 20;

    	chart.dateFormatter.inputDateFormat = "YYYY-MM-dd";

    	var dateAxis = chart.xAxes.push(new am4charts.DateAxis());
    	dateAxis.renderer.grid.template.location = 0;

    	var valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
    	valueAxis.tooltip.disabled = true;

    	var series = chart.series.push(new am4charts.CandlestickSeries());
    	series.dataFields.dateX = "date";
    	series.dataFields.valueY = "close";
    	series.dataFields.openValueY = "open";
    	series.dataFields.lowValueY = "low";
    	series.dataFields.highValueY = "high";
    	series.simplifiedProcessing = true;
    	series.tooltipText = "Open:{openValueY.value}\nLow:{lowValueY.value}\nHigh:{highValueY.value}\nClose:{valueY.value}";

    	// important!
    	// candlestick series colors are set in states. 
    	// series.riseFromOpenState.properties.fill = am4core.color("#00ff00");
    	// series.dropFromOpenState.properties.fill = am4core.color("#FF0000");
    	// series.riseFromOpenState.properties.stroke = am4core.color("#00ff00");
    	// series.dropFromOpenState.properties.stroke = am4core.color("#FF0000");

    	series.riseFromPreviousState.properties.fillOpacity = 1;
    	series.dropFromPreviousState.properties.fillOpacity = 0;

    	chart.cursor = new am4charts.XYCursor();

    	// a separate series for scrollbar
    	var lineSeries = chart.series.push(new am4charts.LineSeries());
    	lineSeries.dataFields.dateX = "date";
    	lineSeries.dataFields.valueY = "close";
    	// need to set on default state, as initially series is "show"
    	lineSeries.defaultState.properties.visible = false;

    	// hide from legend too (in case there is one)
    	lineSeries.hiddenInLegend = true;
    	lineSeries.fillOpacity = 0.5;
    	lineSeries.strokeOpacity = 0.5;

    	var scrollbarX = new am4charts.XYChartScrollbar();
    	scrollbarX.series.push(lineSeries);
    	chart.scrollbarX = scrollbarX;

    	
    }  
 
    
    function renderChart2(security) {
    	security = selectedSecurity;
    	console.log('about to render chart2 on strategyName=MovingMomentumStrategy and security='+security);
    	
     	var dailyPricesUrl = "dailyPrices?security="+security;
     	var tradesUrl = "trades?security="+security+"&strategy=MovingMomentumStrategy";
     	var indicatorValueUrl = "rsiValues?security="+security+"&strategy=MovingMomentumStrategy";

     	console.log("dailyPricesUrl",dailyPricesUrl);
     	console.log("tradesUrl",tradesUrl);
    	console.log("indicatorValueUrl",indicatorValueUrl);
	   		
	    var chart = AmCharts.makeChart( "chart-div", {
	    	  "type": "stock",
		      "theme": "light",
	    	  "color": "#fff",
	    	  "dataSets": [{
	    	    "title": security,
	    	    "fieldMappings": [ {
	    	      "fromField": "open",
	    	      "toField": "open"
	    	    }, {
	    	      "fromField": "high",
	    	      "toField": "high"
	    	    }, {
	    	      "fromField": "low",
	    	      "toField": "low"
	    	    }, {
	    	      "fromField": "close",
	    	      "toField": "close"
	    	    }, {
	    	      "fromField": "volume",
	    	      "toField": "volume"
	    	    } ],
	    	    "compared": false,
	    	    "categoryField": "date",
	    	    "dataLoader": {
	    	    	  "url": dailyPricesUrl,
	    	    	  "format": "json",
	    	    	  "showErrors": true,
	    	    	  "async": false,
	    	    	  "postProcess": function(data, config, chart) {
	    	    	      var newData = [];
	    	    	      for (var i = 0; i < data.rows.length; i++) {
	    	    	        var dataPoint = {};
	    	    	        for (var c = 0; c < data.columns.length; c++) {
	    	    	          dataPoint[data.columns[c]] = data.rows[i][c];
	    	    	        }
	    	    	        newData.push(dataPoint);
	    	    	      }
	    	    	      return newData;
	    	    	  }
	    	    },		    	    
	    	    /**
    	        * data loader for events data
    	        */
    	        "eventDataLoader": {
	    	          "url": tradesUrl,
	    	          "format": "json",
	    	          "showErrors": true,
	    	          "showCurtain": true,
	    	          "async": true,
	    	          "reverse": true,
	    	          "delimiter": ",",
	    	          "useColumnNames": true,	    	          
	    	          "postProcess": function ( data, config, chart) {
		    	            for ( var x in data ) {
		    	              switch( data[x].type ) {
		    	                case 'B':
		    	                  var color = "#00CC00";
		    	                  var type =  "arrowUp";
		    	                  var buysell = "Köp";
		    	                  break;
		    	                default:
		    	                  var color = "#CC0000";
		    	                  var type =  "arrowDown";
		    	                  var buysell = "Sälj";
		    	                  break;
		    	              }
		    	             // data[x].Description = data[x].Description.replace( "Upgrade", "<strong style=\"color: #0c0\">Upgrade</strong>" ).replace( "Downgrade", "<strong style=\"color: #c00\">Downgrade</strong>" );
		    	             // console.log("data[x].date",data[x].date);

		    	              data[x] = {
		    	                type: type,
		    	                graph: "g1",
		    	                backgroundColor: color,
		    	                date: data[x].date,
		    	                text: buysell,
		    	                //description: "<strong>" + data[x].Title + "</strong><br />" + data[x].Description
		    	                description: "Beskrivning"
		    	              };
		    	            }
		    	            return data;
		    	          }
    	       }  //eventDataLoader
	    	  }], //dataset
	    	  "dataDateFormat": "YYYY-MM-DD",	    	  
	    	  "panels": [ {
	    		    "showCategoryAxis": false,
	    		    "title": "Value",
	    		    "percentHeight": 70,
	    		    "stockGraphs": [ {
// 	    		        "type": "candlestick",
// 	    		        "id": "g1",
// 	    		        "openField": "open",
// 	    		        "closeField": "close",
// 	    		        "highField": "high",
// 	    		        "lowField": "low",
// 	    		        "valueField": "close",
// 	    		        "lineColor": "#7f8da9",
// 	    		        "fillColors": "#7f8da9",
// 	    		        "negativeLineColor": "#db4c3c",
// 	    		        "negativeFillColors": "#db4c3c",
// 	    		        "fillAlphas": 1,
// 	    		        "useDataSetColors": false,
// 	    		        "comparable": true,
// 	    		        "compareField": "value",
// 	    		        "showBalloon": false,
// 	    		        "proCandlesticks": true
	    		    	"id": "g1",
	    		      "valueField": "close",
	    		      "comparable": true,
	    		      "compareField": "value",
	    		      "balloonText": "[[title]]:<b>[[value]]</b>",
	    		      "compareGraphBalloonText": "[[title]]:<b>[[value]]</b>"
	    		    } ],
	    		    "stockLegend": {
	    		      "periodValueTextComparing": "[[percents.value.close]]%",
	    		      "periodValueTextRegular": "[[value.close]]"
	    		    }
	    		  }, {
	    		    "title": "Volume",
	    		    "percentHeight": 30,
	    		    "stockGraphs": [ {
	    		      "valueField": "volume",
	    		      "type": "column",
	    		      "showBalloon": false,
	    		      "fillAlphas": 1
	    		    } ],
	    		    "stockLegend": {
	    		      "periodValueTextRegular": "[[value.close]]"
	    		    }
	    		  } ],  //panel

	    		  "chartScrollbarSettings": {
	    		    "graph": "g1"
	    		  },

	    		  "chartCursorSettings": {
	    		    "valueBalloonsEnabled": true,
	    		    "fullWidth": true,
	    		    "cursorAlpha": 0.1,
	    		    "valueLineBalloonEnabled": true,
	    		    "valueLineEnabled": true,
	    		    "valueLineAlpha": 0.5
	    		  },

	    		  "periodSelector": {
	    		    "position": "bottom",
	    		    "periods": [ {
	    		      "period": "MM",
	    		      "count": 1,
	    		      "label": "1 month"
	    		    }, {
	    		      "period": "YYYY",
	    		      "count": 1,
	    		      "label": "1 year"
	    		    }, {
	    		      "period": "YTD",
	    		      "label": "YTD"
	    		    }, {
	    		      "period": "MAX",
	    		      "selected": true,
	    		      "label": "MAX"
	    		    } ]
	    		  },

	    		  "export": {
	    		    "enabled": false
	    		  }
	    		  
	    }); //chart
    	
    }  //renderChart   
        
    
    
    </script>

</body>

</html>
