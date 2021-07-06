function startFeedPriceWTF() {
  console.log("::startFeedPrice::, wsZ",ws);

  var ws = new SockJS("/frosk-analyzer/ws");
  console.log("ws",ws);

  var request_data_interval;

  ws.onopen = function()
  {
      // Web Socket is connected, send data using send()
      requestData();
      request_data_interval = window.setInterval(requestData, 100);

  };

  ws.onmessage = function (evt)
  {
      var received_msg = evt.data;
      data = JSON.parse(evt.data);
      console.log("data",data);

        if(data.type == 'price') {
          var update = {
              x: data.time,
              y: data.value
          }

        //          Plotly.extendTraces('chart-div', update, [0]);
        var my_plot = {
            x: data.time,
            y: data.value,
            type: 'scatter',
        };

          Plotly.newPlot('chart-div', [my_plot]);


        }

  };   //onmessage

  ws.onclose = function()
  {
    // websocket is closed.
    window.clearInterval(request_data_interval)
  };

  function requestData()
  {
      ws.send("ping");
  }

}


function startFeedPrice() {
    console.log("::startFeedPrice::, wsR",ws);

    var ws = new SockJS("/frosk-analyzer/ws");
//     var ws = new WebSocket("ws://localhost:8080/frosk-analyzer/ws");
    console.log("ws",ws);

    var request_data_interval;

    ws.onopen = function()
    {
        // Web Socket is connected, send data using send()
        ws.send("ping");
        request_data_interval = window.setInterval(requestData, 100);

        var trace1 = {
            x: [],
            y: [],
            name: 'Price',
            type: 'scatter'
          };

        var trace2 = {
        x: [],
        y: [],
        xaxis: 'x2',
        yaxis: 'y2',
        name: 'Loi',
        type: 'scatter'
        };

        var layout = {
        xaxis: {
          domain: [0, 1],
          showticklabels: false
        },
        yaxis: {domain: [0.6,1]},
        xaxis2: {
          anchor: 'y2',
          domain: [0, 1]
        },
        yaxis2: {
          anchor: 'x2',
          domain: [0, 0.4]},
        }

        var data = [trace1,trace2];
        Plotly.plot('chart-div',  data, layout);

    };

    ws.onmessage = function (evt)
    {
        var received_msg = evt.data;
        data = JSON.parse(evt.data);

//        console.log("data",data);
        console.log("price",data.price);

        var data;
        let type = data.type;
        if(type == 'price') {
            data2 = data;
            var update = {
                x: [[data2.time], []],
                y: [[data2.price], []]
            }

              var updateNEW = {
                  x: data2.time,
                  y: data2.price
              }



//            Plotly.extendTraces('chart-div', update, [0,1])
         Plotly.prependTraces('chart-div', update, [0,1])

        }
        if(type == 'cusum_high') {
            data2 = data;
            var update = {
                x: [[], [data2.time]],
                y: [[], [data2.value]]
            }

//            Plotly.extendTraces('chart-div', update, [0,1])
            Plotly.prependTraces('chart-div', update, [0,1])

        }

        if(type == 'loi') {
            data2 = data;
            var update = {
                x: [[], [data2.time]],
                y: [[], [data2.value]]
            }

//            Plotly.extendTraces('chart-div', update, [0,1])
            Plotly.prependTraces('chart-div', update, [0,1])

        }


    };   //onmessage

    ws.onclose = function()
    {
      // websocket is closed.
      window.clearInterval(request_data_interval)
    };

    function requestData()
    {
        ws.send("ping");
    }

}
