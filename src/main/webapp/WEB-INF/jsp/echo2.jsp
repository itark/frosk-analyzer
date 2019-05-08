<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<script src="https://cdn.plot.ly/plotly-latest.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.2/jquery.min.js"></script>
<script src="https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
<script>
    ws = new SockJS("ws");
    var request_data_interval;
    
    ws.onopen = function()
    {
        // Web Socket is connected, send data using send()
        ws.send("Message to send");
        request_data_interval = window.setInterval(requestData, 5000);

        var dataInit = [{
            x: [new Date()],
            y: [Math.random()],
            mode: 'scatter',
            line: {color: '#80CAF6'}
        }]

        Plotly.plot('chart', dataInit);

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


//https://plot.ly/javascript/streaming/#streaming-with-timestamp

//alt 1
        //     Plotly.plot('chart',[{
        //             y:[data2.price],
        //             type:'line'
        //     }]);


        //    var cnt = 0;
        //     setInterval(function(){
        //         Plotly.extendTraces('chart',{ y:[[data2.price]]}, [0]);
        //         cnt++;
        //         if(cnt > 500) {
        //             Plotly.relayout('chart',{
        //                 xaxis: {
        //                     range: [cnt-500,cnt]
        //                 }
        //             });
        //         }
        //     },15);
//end alt 1
//alt 2

        // function rand() {
        //     return Math.random();
        // }

        // var time = new Date();

        // var data = [{ x: [time],
        //     y: [rand()],
        //     mode: 'lines',
        //     line: {color: '#80CAF6'}
        // }]


        // Plotly.plot('chart', data);

        // var cnt = 0;

        // var interval = setInterval(function() {

        //     var time = new Date();

        //     var update = {
        //         x:  [[time]],
        //         y: [[rand()]]
        //     }

        //     Plotly.extendTraces('chart', update, [0])

        //     if(cnt === 100) clearInterval(interval);
        // }, 1000);


//end alt 2
//alt 3
        // function rand() {
        // return Math.random();
        // }

        // var time = new Date();

        // var data = [{
        // x: [data2.time],
        // y: [data2.price],
        // mode: 'lines',
        // line: {color: '#80CAF6'}
        // }]


        // Plotly.plot('chart', data);

        // var cnt = 0;

        // var interval = setInterval(function() {

        //     var time = new Date();

        //     var update = {
        //     x:  [[time]],
        //     y: [[rand()]]
        //     }

        //     Plotly.extendTraces('chart', update, [0])

        //     if(cnt === 100) clearInterval(interval);
        // }, 1000);
//end alt 3

//alt 4
        // var data = {
        //     x: [[data2.time]],
        //     y: [[data2.price]]
        // }

        // Plotly.extendTraces('chart', data, [0])
//end alt 4

//alt 4.1

        var data = {
          x: [[new Date()]],
          y: [[data2.price]],
        }

        Plotly.extendTraces('chart', data, [0])


//end alt 4.1


        }

//alt 4.2
        // var data = {
        //   x: [[new Date()]],
        //   y: [[Math.random()]],
        // }

        // Plotly.extendTraces('chart', data, [0])
//end alt 4.2


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
</script>
<body>

<div id="chart" style="width: 800px; height: 800px;">



</div>




</body>


</html>