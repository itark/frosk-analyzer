var selectedSecurity;

function renderTable(dataset) {
		var events = $('#events');
	    var featStratTable = $('#featuredStrategies').DataTable({
	    	responsive: true,
	    	select: true,
	    	destroy: true,
	    	"sAjaxSource": "featuredStrategies?strategy="+strategy+"&dataset="+dataset,
			"sAjaxDataProp": "",
			"order": [[ 1, "desc" ]],
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
	        	var security = featStratTable.rows( indexes ).data().pluck( 'securityName' );
	         	selectedSecurity = security[0];
	
	         	renderChartOHLC();
	        	
	    }) ; 
	
	    featStratTable
			.on( 'draw.dt', function () {
				if(strategy = 'ALL') {
					featStratTable.columns( [0] ).visible( true );
				} else {
					featStratTable.columns( [0] ).visible( false );
				}
		    	let security = featStratTable.rows( 0 ).data().pluck( 'securityName' );
		    	selectedSecurity = security[0];
		        if(selectedSecurity != "") {
//		        	renderChartOHLC();
		        }
	        
			} );     
	    
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
	chart.paddingRight = 20;
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
	chart.paddingRight = 20;
	chart.dateFormatter.inputDateFormat = "YYYY-MM-dd";

	var dateAxis = chart.xAxes.push(new am4charts.DateAxis());
	dateAxis.renderer.grid.template.location = 0;
	dateAxis.renderer.labels.template.fill = am4core.color("#e59165");

	var valueAxis = chart.yAxes.push(new am4charts.ValueAxis());
	valueAxis.tooltip.disabled = true;
	valueAxis.renderer.labels.template.fill = am4core.color("#e59165");

	valueAxis.renderer.minWidth = 60;

	var series = chart.series.push(new am4charts.LineSeries());
	series.name = security;
	series.dataFields.dateX = "date";
	series.dataFields.valueY = "close";
	series.tooltipText = "{valueY.value}";
	series.fill = am4core.color("#e59165");
	series.stroke = am4core.color("#e59165");
	//series.strokeWidth = 3;

	// Add simple bullet//////
//	var bullet = series.bullets.push(new am4charts.Bullet());
//	var square = bullet.createChild(am4core.Rectangle);
//	square.width = 10;
//	square.height = 10;	
	////////////
	
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








 	
