function startFeedPrice() {
    console.log("::startFeedPrice::")

    var ws = new SockJS("ws");
    console.log("ws",ws);
    
    var request_data_interval;
    
    ws.onopen = function()
    {
        // Web Socket is connected, send data using send()
        ws.send("Message to send");



        request_data_interval = window.setInterval(requestData, 5000);



        var dataInit = [{
            x: [],
            y: [],
            mode: 'scatter',
            line: {color: '#DF56F1'}
        }]


        Plotly.plot('chart-div', dataInit);
        

    };
		
    ws.onmessage = function (evt) 
    { 
        var received_msg = evt.data;
        data = JSON.parse(evt.data);
        
        var data2;
        let type = data.type;
        if(type == 'price') {
 
            data2 = data;
            console.log("data2.time",data2.time);
            console.log("data2.price",data2.price);

        var data = {
          x: [[new Date()]],
          y: [[data2.price]],
        }

        Plotly.extendTraces('chart-div', data, [0])


        }


    };   //onmessage
		
    ws.onclose = function()
    { 
      // websocket is closed.
      window.clearInterval(request_data_interval)
    };
    
    function requestData()
    {
        ws.send("get-data");
    }



}