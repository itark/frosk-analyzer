
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
		      	let name = featStratTable.rows( 0 ).data().pluck( 'name' );
		    	let security = featStratTable.rows( 0 ).data().pluck( 'securityName' );
		    	selectedSecurity = security[0];
		        
		        renderChartOHLC();
	        
			} );            
}	
 	
