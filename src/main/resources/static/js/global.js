var selectedSecurity;
var selectedStrategy;

function renderTable(dataset) {
		var events = $('#events');
	    var featStratTable = $('#featuredStrategies').DataTable({
	    	responsive: true,
	    	select: true,
	    	destroy: true,
	    	"sAjaxSource": "featuredStrategies?strategy="+strategy+"&dataset="+dataset,
			"sAjaxDataProp": "",
			"order": [[ 2, "desc" ]],
			"aoColumns": [
				  { "mData": "name"},
				  { "mData": "securityName"},
			      { "mData": "totalProfit"},
				  { "mData": "numberofTrades" },
			      { "mData": "latestTrade" },
				  { "mData": "period"},
			      { "mData": "numberOfTicks" },
				  { "mData": "averageTickProfit" },
				  { "mData": "profitableTradesRatio" },
				  { "mData": "maxDD" }
			]       
	    
	    });
	
	    featStratTable
			.on( 'select', function ( e, dt, type, indexes ) {
	        	var rowData = featStratTable.rows( indexes ).data().toArray();
	        	var name = featStratTable.rows( indexes ).data().pluck( 'name' );
	        	var security = featStratTable.rows( indexes ).data().pluck( 'securityName' );
	         	selectedSecurity = security[0];
	         	selectedStrategy = name[0];
	
//	         	renderChartOHLC();
//	         	renderChartLine();
	         	renderChartLineWithAddons3();
	        	
	    }) ; 
	
	    featStratTable
			.on( 'draw.dt', function () {
				console.log("draw.dt, strategy",strategy)
		    	let security = featStratTable.rows( 0 ).data().pluck( 'securityName' );
		    	selectedSecurity = security[0];
		        if(selectedSecurity != "") {
//		        	renderChartOHLC();
		        }
	        
			} );     
	    

		if(strategy != 'ALL') {
			featStratTable.columns( [0] ).visible( false, false );
			featStratTable.columns.adjust().draw( true );
			console.log("shold set name to hide");
		}
	    
	    
	    
	    $("#dataset").text(dataset);
  
}

function renderChartOHLC() {
	security = selectedSecurity;
	console.log('about to render chart on strategyName=' + strategy
			+ ' and security=' + security);
	var dailyPricesUrl = "dailyPrices?security=" + security;
	var tradesUrl = "trades?security=" + security + "&strategy="
			+ strategy;
	var indicatorValueUrl = "rsiValues?security=" + security
			+ "&strategy=" + strategy;
	console.log("dailyPricesUrl", dailyPricesUrl);
	console.log("tradesUrl", tradesUrl);
	console.log("indicatorValueUrl", indicatorValueUrl);

	am4core.useTheme(am4themes_animated);
	var chart = am4core.create("chart-div", am4charts.XYChart);
	chart.dataSource.url = dailyPricesUrl;
	chart.paddingRight = 5;
	chart.dateFormatter.inputDateFormat = "YYYY-MM-dd";

	var dateAxis = chart.xAxes.push(new am4charts.DateAxis());
	dateAxis.renderer.grid.template.location = 0;
	var valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
	valueAxis.tooltip.disabled = true;

	var dateAxis2 = chart.xAxes.push(new am4charts.DateAxis());
	dateAxis2.renderer.grid.template.location = 0;
	var valueAxis2 = chart.yAxes.push(new am4charts.ValueAxis());
	valueAxis2.tooltip.disabled = true;

	var series = chart.series.push(new am4charts.CandlestickSeries());
	series.name = "Kalle Anka";
	series.dataFields.dateX = "date";
	series.dataFields.valueY = "close";
	series.dataFields.openValueY = "open";
	series.dataFields.lowValueY = "low";
	series.dataFields.highValueY = "high";
	series.simplifiedProcessing = true;
	series.tooltipText = "Open:{openValueY.value}\nLow:{lowValueY.value}\nHigh:{highValueY.value}\nClose:{valueY.value}";

	series.riseFromPreviousState.properties.fillOpacity = 1;
	series.dropFromPreviousState.properties.fillOpacity = 0;

	chart.cursor = new am4charts.XYCursor();

	// a separate series for scrollbar
	var lineSeries = chart.series.push(new am4charts.LineSeries());
	lineSeries.name = "Donald Duck"
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

function renderChartLine() {
	security = selectedSecurity;
	strategy = selectedStrategy;
	console.log('about to render chart on strategyName=' + strategy
			+ ' and security=' + security);
	var dailyPricesUrl = "dailyPrices?security=" + security;
	var tradesUrl = "trades?security=" + security + "&strategy="
			+ strategy;
	var indicatorValueUrl = "rsiValues?security=" + security
			+ "&strategy=" + strategy;
	console.log("dailyPricesUrl", dailyPricesUrl);
	console.log("tradesUrl", tradesUrl);
	console.log("indicatorValueUrl", indicatorValueUrl);

	am4core.useTheme(am4themes_animated);

	var chart = am4core.create("chart-div", am4charts.XYChart);
	
	chart.paddingRight = 5;
	chart.dateFormatter.inputDateFormat = "YYYY-MM-dd";

	var dateAxis = chart.xAxes.push(new am4charts.DateAxis());
	dateAxis.renderer.grid.template.location = 0;
	dateAxis.renderer.labels.template.fill = am4core.color("#e59165");

	var valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
	valueAxis.tooltip.disabled = true;
	valueAxis.renderer.labels.template.fill = am4core.color("#e59165");

	valueAxis.renderer.minWidth = 90;

	var series = chart.series.push(new am4charts.LineSeries());
	series.dataSource.url = dailyPricesUrl; 
	series.name = security;
	series.dataFields.dateX = "date";
	series.dataFields.valueY = "close";
	series.tooltipText = "{valueY.value}";
	series.fill = am4core.color("#e59165");
	series.stroke = am4core.color("#e59165");
	
	chart.cursor = new am4charts.XYCursor();
	chart.cursor.xAxis = dateAxis;

	var scrollbarX = new am4charts.XYChartScrollbar();
	scrollbarX.series.push(series);
	chart.scrollbarX = scrollbarX;

	chart.legend = new am4charts.Legend();
	chart.legend.parent = chart.plotContainer;
	chart.legend.zIndex = 100;

	dateAxis.renderer.grid.template.strokeOpacity = 0.07;
	valueAxis.renderer.grid.template.strokeOpacity = 0.07;
	

}


function renderChartLineWithAddons() {
	security = selectedSecurity;
	strategy = selectedStrategy;
	var dailyPricesUrl = "dailyPrices?security=" + security;
	var tradesUrl = "trades?security=" + security + "&strategy="+ strategy;
	var indicatorValueUrl = "rsiValues?security=" + security+"&strategy=" + strategy;

	console.log('about to render chart on strategyName=' + strategy+' and security=' + security);
	console.log("dailyPricesUrl", dailyPricesUrl);
	console.log("tradesUrl", tradesUrl);
	console.log("indicatorValueUrl", indicatorValueUrl);

	
	var dailyPricesUrl = "dailyPrices?security="+security;
 	var tradesUrl = "trades?security="+security+"&strategy="+strategy;
 	var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;

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
    	  //  "compared": false,
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
	    	                case 'Buy':
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

	    	              data[x] = {
	    	                type: type,
	    	                graph: "g1",
	    	                backgroundColor: color,
	    	                date: data[x].date,
	    	                text: buysell,
	    	                description: "<strong>" + data[x].type + "</strong><br /> price:" + data[x].price
	    	              };
	    	            }
	    	            return data;
	    	          }
	       }  //eventDataLoader
    	  }
    	  ,{
		      "title": strategy,
		      "fieldMappings": [ {
		        "fromField": "value",
		        "toField": "close"
		      } ],
		      "categoryField": "date",
	    	    "dataLoader": {
	    	    	  "url": indicatorValueUrl,
	    	    	  "format": "json",
	    	    	  "showErrors": true,
	    	    	  "async": true
	    	    }			      
		    }    	  
    	  ], //dataset
    	  "dataDateFormat": "YYYY-MM-DD",	    	  
 
    	  "panels": [ {
    		    "showCategoryAxis": false,
    		    "title": "Close",
    		    "percentHeight": 70,
    		    "stockGraphs": [ {
//	    		        "type": "candlestick",
//	    		        "id": "g1",
//	    		        "openField": "open",
//	    		        "closeField": "close",
//	    		        "highField": "high",
//	    		        "lowField": "low",
//	    		        "valueField": "close",
//	    		        "lineColor": "#7f8da9",
//	    		        "fillColors": "#7f8da9",
//	    		        "negativeLineColor": "#db4c3c",
//	    		        "negativeFillColors": "#db4c3c",
//	    		        "fillAlphas": 1,
//	    		        "useDataSetColors": false,
//	    		        "comparable": true,
//	    		        "compareField": "close",
//	    		        "showBalloon": false,
//	    		        "proCandlesticks": true,
    			      "id": "g1",
    			      "valueField": "close",
    			      "comparable": true,
    			      "compareField": "close",
    			      "balloonText": "[[title]]:<b>[[close]]</b>",
    			      "compareGraphBalloonText": "[[title]]:<b>[[value]]</b>"
    		    } ],
    		    "stockLegend": {
    		      "periodValueTextComparing": "[[percents.value.close]]%",
    		      "periodValueTextRegular": "[[value.close]]"
    		    }
    		  }, 
    		  {
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
    		  }
    		  ],  //panel

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
    		      "selected": true,
    		      "count": 1,
    		      "label": "1 year"
    		    }, {
    		      "period": "YTD",
    		      "label": "YTD"
    		    }, {
    		      "period": "MAX",
    		      "label": "MAX"
    		    } ]
    		  },

    		  "export": {
    		    "enabled": false
    		  }
    		  
    });
    
    $("#charttype").text('Line addons');
    
    
}

function renderChartLineWithAddons3() {

	security = selectedSecurity;
	strategy = selectedStrategy;

	console.log('about to render chart on strategyName=' + strategy+' and security=' + security);
	
	var dailyPricesUrl = "dailyPrices?security="+security;
 	var tradesUrl = "trades?security="+security+"&strategy="+strategy;
 	var indicatorValueUrl = "indicatorValues?security="+security+"&strategy="+strategy;

 	console.log("dailyPricesUrl",dailyPricesUrl);
 	console.log("tradesUrl",tradesUrl);
	console.log("indicatorValueUrl",indicatorValueUrl);
	
	
	
	var chart = AmCharts.makeChart( "chart-div", {
		  "type": "stock",
		  "theme": "light",
		  "color": "#fff",
		  "dataSets": [ {
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
  	    	  "async": true
    	    },	    

		    /**
		     * data loader for events data
		     */
		    "eventDataLoader": {
		      "url": tradesUrl,
		      "format": "json",
		      "showCurtain": true,
		      "showErrors": true,
		      "async": true,
		      "reverse": true,
		      "delimiter": ",",
		      "useColumnNames": true,
		      "postProcess": function( data ) {
		    	 // console.log('data3',data);
  	            for ( var x in data ) {
  	              switch( data[x].type ) {
  	                case 'Buy':
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

  	              data[x] = {
  	                type: type,
  	                graph: "g1",
  	                backgroundColor: color,
  	                date: data[x].date,
  	                text: buysell,
  	                description: "<strong>" + data[x].type + "</strong><br /> price:" + data[x].price
  	              };
		        }
		        return data;
		      }
		    }

		  }, {
		    "title": strategy,
		    "fieldMappings": [ {
		      "fromField": "value",
		      "toField": "close"
		    } ],
		    "compared": true,
		    "categoryField": "date",
	  	    "dataLoader": {
		    	  "url": indicatorValueUrl,
		    	  "format": "json",
		    	  "showErrors": true,
		    	  "async": true
		    }			      
		  } ],
		  "dataDateFormat": "YYYY-MM-DD",

		  "panels": [ {
		      "title": "Close",
		      "percentHeight": 70,
		      "stockGraphs": [ {
			      "id": "g1",
			      "valueField": "close",
			      "comparable": false,
			      "compareField": "close",
			      "balloonText": "[[title]]:<b>[[close]]</b>",
			      "compareGraphBalloonText": "[[title]]:<b>[[value]]</b>"
		      } ],

		      "stockLegend": {
		        "valueTextRegular": undefined,
		        "periodValueTextComparing": "[[percents.value.close]]%"
		      }

		    },

  		  	{
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
    		 },

    		 {
		      "title": strategy,
		      "percentHeight": 30,
		      "marginTop": 1,
		      "showCategoryAxis": false,
		      "stockGraphs": [ {
			      "id": "g1",
			      "valueField": "close",
			      "comparable": true,
			      "compareField": "close",
			      "balloonText": "[[title]]:<b>[[close]]</b>",
			      "compareGraphBalloonText": "[[title]]:<b>[[value]]</b>"
		      } ],
		      "stockLegend": {
		        "markerType": "bubble",
		        "markerSize": 12,
		        "labelText": "[[title]]",
		        "periodValueTextComparing": "[[value.close]]"
		      },

		      "valueAxes": [ {
		        "usePrefixes": true
		      } ]
		    }

    		],

//		  "panelsSettings": {
//		      //  "color": "#fff",
//		    "plotAreaFillColors": "#333",
//		    "plotAreaFillAlphas": 1,
//		    "marginLeft": 60,
//		    "marginTop": 5,
//		    "marginBottom": 5
//		  },

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
		  
//		  "chartScrollbarSettings": {
//		    "graph": "g1",
//		    "graphType": "line",
//		    "usePeriod": "WW",
//		    "backgroundColor": "#333",
//		    "graphFillColor": "#666",
//		    "graphFillAlpha": 0.5,
//		    "gridColor": "#555",
//		    "gridAlpha": 1,
//		    "selectedBackgroundColor": "#444",
//		    "selectedGraphFillAlpha": 1
//		  },

//		  "categoryAxesSettings": {
//		    "equalSpacing": true,
//		    "gridColor": "#555",
//		    "gridAlpha": 1
//		  },
//
//		  "valueAxesSettings": {
//		    "gridColor": "#555",
//		    "gridAlpha": 1,
//		    "inside": false,
//		    "showLastLabel": true
//		  },
//
//		  "chartCursorSettings": {
//		    "pan": true,
//		    "valueLineEnabled": true,
//		    "valueLineBalloonEnabled": true
//		  },
//
//		  "legendSettings": {
//		    //"color": "#fff"
//		  },
//
//		  "stockEventsSettings": {
//		    "showAt": "high",
//		    "type": "pin"
//		  },
//
//		  "balloon": {
//		    "textAlign": "left",
//		    "offsetY": 10
//		  },

		  "periodSelector": {
		    "position": "bottom",
		    "periods": [ {
		        "period": "DD",
		        "count": 10,
		        "label": "10D"
		      }, {
		        "period": "MM",
		        "count": 1,
		        "label": "1M"
		      }, {
		        "period": "MM",
		        "count": 6,
		        "label": "6M"
		      }, {
		        "period": "YYYY",
		        "count": 1,
		        "label": "1Y"
		      }, {
		        "period": "YYYY",
		        "count": 2,
		        "label": "2Y"
		      },
		      {
		        "period": "YTD",
		        "label": "YTD"
		      },
		      {
		        "period": "MAX",
		        "selected": true,
		        "label": "MAX"
		      }
		    ]
		  }
		} );	
	
    $("#charttype").text('Line addons3');
	
}

