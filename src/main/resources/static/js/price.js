

function startFeedPrice() {
    console.log("::startFeedPrice::, ws",ws);

    var ws = new SockJS("/frosk-analyzer/ws");
    console.log("ws",ws);

    var request_data_interval;

    ws.onopen = function()
    {
        // Web Socket is connected, send data using send()
        ws.send("ping");
        request_data_interval = window.setInterval(requestData, 1000);

        var trace1 = {
            x: [],
            y: [],
            name: 'Price',
            type: 'scatter'
          };

        //   var trace2 = {
        //     x: [],
        //     y: [],
        //     name: 'Change detection',
        //     xaxis: 'x2',
        //     yaxis: 'y2',
        //     type: 'scatter',
        //     mode: 'markers',
        //   };


          var trace2 = {
            x: [],
            y: [],
            xaxis: 'x2',
            yaxis: 'y2',
            name: 'Loi',
            type: 'scatter'
          };

        //   var layout = {
        //     title: 'Price with change events',
        //     yaxis: {title: 'Price'},
        //     yaxis2: {
        //       title: 'Change detection',
        //       titlefont: {color: 'rgb(148, 103, 189)'},
        //       tickfont: {color: 'rgb(148, 103, 189)'},
        //       overlaying: 'y',
        //       side: 'right'
        //     }
        //   };

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


        // var data = [trace1,trace2, trace3];
        var data = [trace1,trace2];

        Plotly.plot('chart-div',  data, layout);

    };

    ws.onmessage = function (evt)
    {
        var received_msg = evt.data;
        data = JSON.parse(evt.data);

        console.log("data",data);
        //var now = new Date();

        var data2;
        let type = data.type;
        if(type == 'price') {
            data2 = data;
            var update = {
                // x: [[data2.time], [],[]],
                // y: [[data2.price], [],[]]
                x: [[data2.time], []],
                y: [[data2.price], []]
            }

            // Plotly.extendTraces('chart-div', update, [0,1,2])
            Plotly.extendTraces('chart-div', update, [0,1])


        }
        if(type == 'cusum_high') {
            data2 = data;
            var update = {
                x: [[], [data2.time]],
                y: [[], [data2.value]]
            }

            // Plotly.extendTraces('chart-div', update, [0,1,2])
            Plotly.extendTraces('chart-div', update, [0,1])

        }

        if(type == 'loi') {
            data2 = data;
            var update = {
                x: [[], [data2.time]],
                y: [[], [data2.value]]
            }

            Plotly.extendTraces('chart-div', update, [0,1])

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
