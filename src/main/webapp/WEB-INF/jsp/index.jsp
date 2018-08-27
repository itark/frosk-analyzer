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
<!--  
	<nav class="navbar navbar-default">
	  <div class="container-fluid">
	    
	    <div class="navbar-header">
	      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
	        <span class="sr-only">Toggle navigation</span>
	        <span class="icon-bar"></span>
	        <span class="icon-bar"></span>
	        <span class="icon-bar"></span>
	      </button>
	      <a class="navbar-brand" href="#" title="powered by Har-em Foundations">Evening Star</a>
	    </div>
	
	   
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <li class="active"><a href="#">Översikt <span class="sr-only">(current)</span></a></li>
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Strategies <span class="caret"></span></a>
	          <ul class="dropdown-menu">
	            <li><a href="rnn">RNN</a></li>
	            <li role="separator" class="divider"></li>
	            <li><a href="strategies">RSI</a></li>
	          </ul>
	        </li>
	      </ul>
	    </div><
	  </div>
	</nav>
-->

    <nav class="navbar navbar-expand-lg navbar-dark bg-dark fixed-top" id="mainNav">
      <div class="container">
        <a class="navbar-brand js-scroll-trigger" href="#page-top">Start Bootstrap</a>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarResponsive" aria-controls="navbarResponsive" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarResponsive">
          <ul class="navbar-nav ml-auto">
            <li class="nav-item">
              <a class="nav-link js-scroll-trigger" href="#about">About</a>
            </li>
            <li class="nav-item">
              <a class="nav-link js-scroll-trigger" href="#services">Services</a>
            </li>
            <li class="nav-item">
              <a class="nav-link js-scroll-trigger" href="#contact">Contact</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>










    <header class="bg-primary text-white">
      <div class="container text-center">
        <h1>Welcome to Scrolling Nav</h1>
        <p class="lead">A landing page template freshly redesigned for Bootstrap 4</p>
      </div>
    </header> 
  
    <section id="about">
      <div class="container">
        <div class="row">
          <div class="col-lg-10 mx-auto">
            <h2>About this page</h2>
            <p class="lead">This is a great place to talk about your webpage. This template is purposefully unstyled so you can use it as a boilerplate or starting point for you own landing page designs! This template features:</p>
            <ul>
              <li>Clickable nav links that smooth scroll to page sections</li>
              <li>Responsive behavior when clicking nav links perfect for a one page website</li>
              <li>Bootstrap's scrollspy feature which highlights which section of the page you're on in the navbar</li>
              <li>Minimal custom CSS so you are free to explore your own unique design options</li>
            </ul>
          </div>
        </div>
      </div>
    </section>  
  
     <section id="services" class="bg-light">
      <div class="container">
        <div class="row">
          <div class="col-lg-10 mx-auto">
            <h2>Services we offer</h2>
            <p class="lead">Lorem ipsum dolor sit amet, consectetur adipisicing elit. Aut optio velit inventore, expedita quo laboriosam possimus ea consequatur vitae, doloribus consequuntur ex. Nemo assumenda laborum vel, labore ut velit dignissimos.</p>
          </div>
        </div>
      </div>
    </section> 
  
     <section id="contact">
      <div class="container">
 
        <div class="row">
          <div class="col-lg-10 mx-auto">
            <h2>Contact us</h2>
            <p class="lead">Lorem ipsum dolor sit amet, consectetur adipisicing elit. Vero odio fugiat voluptatem dolor, provident officiis, id iusto! Obcaecati incidunt, qui nihil beatae magnam et repudiandae ipsa exercitationem, in, quo totam.</p>
          </div>
        </div>
        <div class="row">
	        <div class="col-12">
	            <h1 class="page-header">Strategier</h1>
	        </div> 
	    </div>
        <div class="row">
           <div class="col-10">
               <div class="panel panel-default">
                   <div class="panel-heading">
                       Strategier på enskilda tidserier
                   </div>
                   
                   <div class="panel-body">
                     <table class="table table-striped table-bordered table-hover" id="featuredStrategies">
		  <thead>
	            <tr>
	                <th>Name</th>
	                <th>Security</th>
	                <th>PeriodDescription</th>
	                <th>TotalProfit %</th>
					<th>NumberOfTicks</th>
	                <th>AverageTickProfit</th>
	                <th>NumberofTrades</th>
	                <th>ProfitableTradesRatio</th>
	                <th>MaxDD</th>
	                <th>LatestTradeDate</th>				                
	            </tr>
	        </thead>
	        <tfoot>
	            <tr>
	                <th>Name</th>
	                <th>Security</th>
	                <th>PeriodDescription</th>
	                <th>TotalProfit %</th>
					<th>NumberOfTicks</th>
	                <th>AverageTickProfit</th>
	                <th>NumberofTrades</th>
	                <th>ProfitableTradesRatio</th>
	                <th>MaxDD</th>
	                <th>LatestTradeDate</th>
	            </tr>
	        </tfoot>

                     </table>
                   </div>
               </div>
           </div>
       </div>
 
 
 
 
      </div>
    </section>
 
 
  
  
         <div class="row">
             <div class="col-12">
                 <h1 class="page-header">Strategier</h1>
             </div>
         </div>
         
      <div class="row">
		<div class="col-12 dc-chart" id="chart-div"></div>
	  </div>    

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

    <!-- Page-Level Demo Scripts - Tables - Use for reference -->
    <script>
    $(document).ready(function() {
    	var events = $('#events');
        var featStratTable = $('#featuredStrategies').DataTable({
        	responsive: true,
        	select: true,
        	"sAjaxSource": "featuredStrategies?strategy=ALL",
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
				  { "mData": "maxDD" },
				  { "mData": "latestTradeDate" }
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
    	
     	var dailyPricesUrl = "dailyPrices?security="+security+"&database=YAHOO";
     	var tradesUrl = "trades?security="+security+"&strategy="+strategyName+"&database=YAHOO";

     	console.log("dailyPricesUrl",dailyPricesUrl);

     	console.log("tradesUrl",tradesUrl);
	   		
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
