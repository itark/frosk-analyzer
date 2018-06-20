<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!--  %@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%-->
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<html lang="en">

<head>

    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>Evening Star</title>

    <!-- DataTables CSS -->
    <link href="webjars/datatables/css/dataTables.bootstrap.css" rel="stylesheet">
 
    <!-- DataTables Responsive CSS -->
    <link href="https://cdn.datatables.net/responsive/1.0.6/css/dataTables.responsive.css" rel="stylesheet">
    <link href="https://cdn.datatables.net/select/1.2.3/css/select.dataTables.min.css" rel="stylesheet">

 	<link rel="stylesheet" href="https://www.amcharts.com/lib/3/plugins/export/export.css" type="text/css" media="all" />
 	
    

</head>

<style>


#chart-div {
	width		: 100%;
	height		: 500px;
	font-size	: 11px;
}
</style>



<body>

    <div id="wrapper">

        <!-- Navigation -->
        <nav class="navbar navbar-default navbar-static-top" role="navigation" style="margin-bottom: 0">
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/">Evening Star</a>
            </div>
            <!-- /.navbar-header -->

            <ul class="nav navbar-top-links navbar-right">
                <li class="dropdown">
            </ul>


               <div class="navbar-default sidebar" role="navigation">
                <div class="sidebar-nav navbar-collapse">
                    <ul class="nav" id="side-menu">
                        <li>
                            <a href="/"><i class="fa fa-dashboard fa-fw"></i> Översikt</a>
                        </li>
                        <li>
                            <a href="/"><i class="fa fa-bar-chart-o fa-fw"></i> Strategier<span class="fa arrow"></span></a>
                            <ul class="nav nav-second-level">
                                <li>
                                    <a href="strategies">RSI for FSE</a>
                                </li>
                            </ul>
                            <ul class="nav nav-second-level">
                                <li>
                                    <a href="rnn">RNN Prediction</a>
                                </li>
                            </ul>  
                            <!-- /.nav-second-level -->
                        </li>
                    </ul>
                </div>
                <!-- /.sidebar-collapse -->
            </div>
         <!-- /.navbar-static-side -->
        </nav>

        <div id="page-wrapper">
            <div class="row">
                <div class="col-lg-12">
                    <h1 class="page-header">RSI</h1>
                </div>
                <!-- /.col-lg-12 -->
            </div>
            <!-- /.row -->
            <div class="row">
                <div class="col-lg-12">
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            RSI på Frankfurt Stock Exchange
                        </div>
                        <!-- /.panel-heading -->
                        
                        <div class="panel-body">
	                         <table width="100%" class="table table-striped table-bordered table-hover" id="featuredStrategies">
							  <thead>
						            <tr>
						                <th>Name</th>
						                <th>Security</th>
						                <th>PeriodDescription</th>
						                <th>TotalProfit</th>
										<th>NumberOfTicks</th>
						                <th>AverageTickProfit</th>
						                <th>NumberofTrades</th>
						                <th>ProfitableTradesRatio</th>
						                <th>MaxDD</th>
						            </tr>
						        </thead>
						        <!-- Footer Table -->
						        <tfoot>
						            <tr>
						                <th>Name</th>
						                <th>Security</th>
						                <th>PeriodDescription</th>
						                <th>TotalProfit</th>
										<th>NumberOfTicks</th>
						                <th>AverageTickProfit</th>
						                <th>NumberofTrades</th>
						                <th>ProfitableTradesRatio</th>
						                <th>MaxDD</th>
						            </tr>
						        </tfoot>
	
	                         </table>
                        </div>
                    </div>
                    <!-- /.panel -->
                </div>
                <!-- /.col-lg-12 -->
            </div>
            
             <div class="row">
				<div class="col-lg-12 dc-chart" id="chart-div"></div>
 			</div>    
   
            <!-- /.row -->
        </div>
        <!-- /#page-wrapper -->
    </div>
    <!-- /#wrapper -->

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

    <!-- Page-Level Demo Scripts - Tables - Use for reference -->
    <script>
    $(document).ready(function() {
    	var events = $('#events');
        var featStratTable = $('#featuredStrategies').DataTable({
        	responsive: true,
        	select: true,
        	"sAjaxSource": "featuredStrategies?strategy=RSI2Strategy&database=FSE",
			"sAjaxDataProp": "",
			"order": [[ 3, "desc" ]],
			"aoColumns": [
				  { "mData": "name"},
				  { "mData": "security"},
				  { "mData": "periodDescription"},
			      { "mData": "totalProfit"},
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
            	var name = featStratTable.rows( indexes ).data().pluck( 'name' );
            	var security = featStratTable.rows( indexes ).data().pluck( 'security' );
            	renderChart(name[0], security[0]);
            	
        }) ; 
        
    });
    

    function renderChart(strategyName, security) {
    	console.log('about to render chart on strategyName='+strategyName+ ' and security='+security);

     	var dailyPricesUrl = "dailyPrices?security="+security+"&database=FSE";
     //	var tradesUrl = "trades?security="+security+"&strategy="+strategyName+"&database=FSE";
    			
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
	    	    	  "async": true,
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
	    	    }
	    	  //,		    	    
    	       /**
    	        * data loader for events data
    	        */
    	        /**
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
		    	              console.log("data[x].date",data[x].date);

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
    	       }
	    	    */

	    	  }], //dataset
  
	    	  "dataDateFormat": "YYYY-MM-DD",	    	  
	    	  "panels": [ {
	    		    "showCategoryAxis": false,
	    		    "title": "Value",
	    		    "percentHeight": 70,
	    		    "stockGraphs": [ {
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
    	
    }  //renderIndiceChart
    
    
    </script>

</body>

</html>
