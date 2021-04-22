var ws;
var stompClient = null;

function connect() {
    var socket = new SockJS('/frosk-analyzer/gs-guide-websocket');
    console.log('socket...')
    stompClient = Stomp.over(socket);
    console.log('stompClient...')
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/greetings', function (greeting) {
            var fromServer = JSON.parse(greeting.body).content;
            console.log("fromServer",fromServer);
        });
    });
}


function setConnected(connected) {
    console.log("::setConnected::",connected);
}


function startFeedLab() {
    connect();

    stompClient.send("app/hello", "helllo....");

}


function startFeedCusum() {
  console.log("::startFeedCusum::, ws",ws);

  ws = new SockJS("ws");
  console.log("ws",ws);
  
  var request_data_interval;
  
  ws.onopen = function()
  {
      // Web Socket is connected, send data using send()
      ws.send("ping");
      request_data_interval = window.setInterval(requestData, 5000);

      var trace1 = {
          x: [],
          y: [],
          name: 'Cusum',
          type: 'scatter'
        };
        
      var layout = {
        title: 'Cusum from Will F',
        yaxis: {title: 'Cusum'},
        yaxis2: {
          title: 'Change detection',
          titlefont: {color: 'rgb(148, 103, 189)'},
          tickfont: {color: 'rgb(148, 103, 189)'},
          overlaying: 'y',
          side: 'right'
        }
      };

      var data = [trace1];  

      Plotly.plot('chart-div',  data, layout);

  };
  
  ws.onmessage = function (evt) 
  { 
      var received_msg = evt.data;
      data = JSON.parse(evt.data);
      console.log("data",data);

      if(data.type == 'cusum_high') {
          var update = {
              x: [[data.time]],
              y: [[data.value]]
          }

          Plotly.extendTraces('chart-div', update, [0]);

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

function startFeedLoi() {
  console.log("::startFeedLoi::, ws",ws);

  ws = new SockJS("ws");
  console.log("ws",ws);
  
  var request_data_interval;
  
  ws.onopen = function()
  {
      // Web Socket is connected, send data using send()
      requestData();
      request_data_interval = window.setInterval(requestData, 10000);

      var trace1 = {
          x: [],
          y: [],
          name: 'Loi',
          type: 'scatter'
        };
        
      var layout = {
        title: 'Loi from Fredrik M',
        yaxis: {title: 'Limit Order Imbalance'},
        yaxis2: {
          title: 'Change detection',
          titlefont: {color: 'rgb(148, 103, 189)'},
          tickfont: {color: 'rgb(148, 103, 189)'},
          overlaying: 'y',
          side: 'right'
        }
      };

      var data = [trace1];  

      Plotly.plot('chart-div',  data, layout);

  };
  
  ws.onmessage = function (evt) 
  { 
      var received_msg = evt.data;
      data = JSON.parse(evt.data);
      console.log("data",data);

      if(data.type == 'loi') {
          var update = {
              x: [[data.time]],
              y: [[data.value]]
          }

          Plotly.extendTraces('chart-div', update, [0]);

      }

  };   //onmessage
  
  ws.onclose = function()
  { 
    // websocket is closed.
    window.clearInterval(request_data_interval)
  };
  
  function requestData()
  {
       console.log("KILROY")

      ws.send("ping");
  }

}


