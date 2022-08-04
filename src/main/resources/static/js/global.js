var selectedSecurity;
var selectedStrategy;

function maybeDisposeRoot(divId) {
  am5.array.each(am5.registry.rootElements, function(root) {
    if (root.dom.id == divId) {
      root.dispose();
    }
  });
}

function renderTable(dataset, divId) {
		var events = $('#events');
	    var featStratTable = $('#featuredStrategies').DataTable({
	    	responsive: true,
	    	select: true,
	    	destroy: true,
	    	"sAjaxSource": "featuredStrategies/"+strategy+"/"+dataset,
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
	            if (selectedStrategy === 'MovingMomentumStrategy') {
	            	maChart(divId);
	            } else if (selectedStrategy === 'RSI2Strategy') {
                    rsi2Chart(divId);
           	    } else if (selectedStrategy === 'EngulfingStrategy') {
                   maChart(divId);  //TODO
	            } else {
	                console.log("Error on " + selectedStrategy);
	            }
	    }) ;
	
	    featStratTable
			.on( 'draw.dt', function () {
				console.log("draw.dt, strategy",strategy)
		    	let security = featStratTable.rows( 0 ).data().pluck( 'securityName' );
		    	selectedSecurity = security[0];
		        if(selectedSecurity != "") {
                    //root.dispose();
                    //root = am5.Root.new("chart-div");
		        }
	        
			} );     

		if(strategy != 'ALL') {
			featStratTable.columns( [0] ).visible( false, false );
			featStratTable.columns.adjust().draw( true );
			console.log("shold set name to hide");
		}
	    
	    $("#dataset").text(dataset);
  
}