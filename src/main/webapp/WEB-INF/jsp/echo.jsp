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
        
        //console.log("data",data);
        
        var data2;
        let type = data.type;
        if(type == 'price') {
 
            data2 = data;
            console.log("data2.time",data2.time);
            console.log("data2.price",data2.price);

            var my_plot2 = {
                x: data2.time, 
                y: data2.price, 
                type: 'scatter',
            }; 


            var my_plot3 = {
                x: ['2013-10-04 22:23:00', '2013-11-04 22:23:00', '2013-12-04 22:23:00'],
                 y: [1, 3, 6],
                type: 'scatter',
            }; 


            var my_plot3 = {
                x: ['2019-05-03T12:38:58.423000Z', '2019-05-03T12:39:13.784000Z', '019-05-03T12:39:38.805000Z'],
                 y: [4873.90000000, 5390.40000000, 5590.40000000],
                type: 'scatter',
            }; 


            Plotly.newPlot('sine-graph', [my_plot3]);

        }

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