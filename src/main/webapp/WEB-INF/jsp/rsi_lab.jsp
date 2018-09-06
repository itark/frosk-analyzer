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
	height		: 700px;
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
            <h2>The Relative Strength Index - LAB</h2>
          </div>
        </div>

        <div class="row">

			<div class="col-lg-9 col-md-9">
				 <div class="col-12 dc-chart" id="chart-div"></div>
			</div>
  
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
					                <th>Security</th>
					                <th>Profit %</th>
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
    $(document).ready(function() {
    	var events = $('#events');
        var featStratTable = $('#featuredStrategies').DataTable({
        	responsive: true,
        	select: true,
        	"sAjaxSource": "featuredStrategies?strategy=RSI2Strategy",
			"sAjaxDataProp": "",
			"order": [[ 1, "desc" ]],
			"aoColumns": [
				  { "mData": "security"},
			      { "mData": "totalProfit"},
				  { "mData": "numberofTrades" },
			      { "mData": "latestTradeDate" },
				  { "mData": "periodDescription"},
			      { "mData": "numberOfTicks" },
				  { "mData": "averageTickProfit" },
				  { "mData": "profitableTradesRatio" },
				  { "mData": "maxDD" }
			]
        });
   
        featStratTable
    		.on( 'select', function ( e, dt, type, indexes ) {
            	let rowData = featStratTable.rows( indexes ).data().toArray();
            	let name = featStratTable.rows( indexes ).data().pluck( 'name' );
            	let security = featStratTable.rows( indexes ).data().pluck( 'security' );
            	renderChart(name[0], security[0]);
            	
        }) ; 
        
        
        featStratTable
        	.on( 'draw.dt', function () {
		      	let name = featStratTable.rows( 0 ).data().pluck( 'name' );
		    	let security = featStratTable.rows( 0 ).data().pluck( 'security' );
		        console.log("name",name);
		        console.log("security",security);
		
		    	console.log("name[0]",name[0]);
		        console.log("security[0]",security[0]);
		        
		        renderChart(name[0], security[0]);
		        
        } );        
        
        
    });
    

    function renderChart(strategyName, security) {
    	console.log('about to render chart on strategyName='+strategyName+ ' and security='+security);
    	
     	var dailyPricesUrl = "dailyPrices?security="+security;
     	var tradesUrl = "trades?security="+security+"&strategy="+strategyName;
     	var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategyName;

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
		    	                  var buysell = "K�p";
		    	                  break;
		    	                default:
		    	                  var color = "#CC0000";
		    	                  var type =  "arrowDown";
		    	                  var buysell = "S�lj";
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
