var stompClient = null;

function connect() {
    var socket = new SockJS('/frosk-analyzer/websocket-broker');
    console.log('socket...')
    stompClient = Stomp.over(socket);
    console.log('stompClient...')
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/price', function (price) {
            var fromServer = JSON.parse(price.body).content;
            console.log("fromServer",fromServer);
        });
    });
}

function startFeedPriceNEW() {

    console.log("startFeedPrice i stomp.js");

    connect();
  //  stompClient.send("app/hello", "helllo....");
}

function setConnected(connected) {
    console.log("::setConnected::",connected);
}






