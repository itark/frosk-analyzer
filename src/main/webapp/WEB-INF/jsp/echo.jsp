<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script src="https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
<script>
    ws = new SockJS("ws")
    var request_data_interval
    ws.onopen = function()
    {
        // Web Socket is connected, send data using send()
        ws.send("Message to send");
        request_data_interval = window.setInterval(requestData, 50);
    };
		
    ws.onmessage = function (evt) 
    { 
        var received_msg = evt.data;
        
        data = JSON.parse(evt.data);
        
        console.log("data",data);
        
        
        var my_plot = {
            x: data.x, 
            y: data.y, 
            type: 'scatter',
        };
        
        
        var my_plot2 = {
            x: data, 
            y: data, 
            type: 'scatter',
        };       
        
        
        
        
        Plotly.newPlot('sine-graph', [my_plot2]);
    };
		
    ws.onclose = function()
    { 
      // websocket is closed.
      window.clearInterval(request_data_interval)
    };
    
    function requestData()
    {
        ws.send("get-data");
    }
</script>
<body>

<div id="sine-graph" style="width: 400px; height: 400px;">



</div>




</body>


</html>