package nu.itark.frosk.coinbase.exchange.api.websocketfeed;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nu.itark.frosk.changedetection.ChangeDetector;
import nu.itark.frosk.coinbase.exchange.api.exchange.Signature;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.HeartBeat;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderBookMessage;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderDoneOrderBookMessage;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderMatchOrderBookMessage;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderOpenOrderBookMessage;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.OrderReceived;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Subscribe;


@Service
@ClientEndpoint
public class WebsocketFeed {

    static Logger log = LoggerFactory.getLogger(WebsocketFeed.class);

    Signature signature;
    Session userSession = null;
    MessageHandler messageHandler;

    String websocketUrl;
    String passphrase;
    String key;
    boolean guiEnabled;
    
    
    @Autowired
    ChangeDetector<Double> changeDetector;
    
    @Autowired
    Observations observations;
    

    @Autowired
	public WebsocketFeed(@Value("${websocket.baseUrl}") String websocketUrl, @Value("${gdax.key}") String key,
			@Value("${gdax.passphrase}") String passphrase, @Value("${gui.enabled}") boolean guiEnabled,
			Signature signature) {

		this.key = key;
		this.passphrase = passphrase;
		this.websocketUrl = websocketUrl;
		this.signature = signature;
		this.guiEnabled = guiEnabled;

		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();

			log.trace("websocketUrl="+websocketUrl);

			container.connectToServer(this, new URI(websocketUrl));

            log.trace("container="+container);


		} catch (Exception e) {
			System.out
					.println("Could not connect to remote server: " + e.getMessage() + ", " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        log.trace("WebsocketFeed::onClose, reason {}:", reason.getReasonPhrase());
        this.userSession = null;
    }

    /**
     * Callback hook for OrderBookMessage Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        log.trace("onMessage, message {}", message);
        if (this.messageHandler != null) {
            this.messageHandler.handleMessage(message);
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {
        log.trace("sendMessage, message : {}", message);
        this.userSession.getAsyncRemote().sendText(message);
    }



    public void subscribe(Subscribe msg) {
        log.trace("msg="+ ReflectionToStringBuilder.toString(msg, ToStringStyle.MULTI_LINE_STYLE));

    	String jsonSubscribeMessage = signObject(msg);
        observations.setProductId(msg.getProduct_ids()[0]);

        addMessageHandler(json -> {
//                    log.info("json="+json);

                    OrderBookMessage message = getObject(json, new TypeReference<OrderBookMessage>() {});
                    if (Objects.isNull(message)) {
                        log.info("oopps, not good, gå vidare");
                        return;
                    }
                    String type = message.getType();

                    if (type.equals("heartbeat"))
                    {
                        HeartBeat heartbeat = getObject(json, new TypeReference<HeartBeat>() {});
                        log.info("heartbeat", heartbeat);
                    }
                    else if (type.equals("received"))
                    {
                        // received orders are not necessarily live orders - so I'm ignoring these msgs as they're
                        // subject to change.
                        //log.info("order received {}", message);
                        OrderReceived orderReceived = getObject(json, new TypeReference<OrderReceived>() {});
                        if (orderReceived.getOrder_type().equals(OrderReceived.OrderTypeEnum.LIMIT.getValue())) {
        
                            observations.synchronizeBest(orderReceived);
                            observations.process(orderReceived);

                        }                        
                    }
                    else if (type.equals("open"))
                    {
                        OrderOpenOrderBookMessage open = getObject(json, new TypeReference<OrderOpenOrderBookMessage>() {});
                        // log.info("Order opened: " + open );
                    }
                    else if (type.equals("done"))
                    {
                        if (!message.getReason().equals("filled")) {
                            OrderBookMessage doneOrder = getObject(json, new TypeReference<OrderDoneOrderBookMessage>() {});
//                            log.info("Order done: " + doneOrder.toString());
                        }
                    }
                    else if (type.equals("match"))
                    {
                        OrderBookMessage matchedOrder = getObject(json, new TypeReference<OrderMatchOrderBookMessage>(){});
                        //log.info("Order matched: " + matchedOrder);
                        observations.setMidMarketPrice(matchedOrder.getPrice());
                        observations.sendPriceMessage(matchedOrder.getPrice().toString(), matchedOrder.getTime());
                    }
                    else if (type.equals("change"))
                    {
                        // TODO - possibly need to provide implementation for this to work in real time.
//                         log.info("Order Changed {}", json);
                        // orderBook.updateOrderBookWithChange(getObject(json, new TypeReference<OrderChangeOrderBookMessage>(){}));
                    }
                    else
                    {
                        // Not sure this is required unless I'm attempting to place orders
                        // ERROR
                        log.error("Error {}", json);
                        // orderBook.orderBookError(getObject(json, new TypeReference<ErrorOrderBookMessage>(){}));
                    }
        	
        });

        // send message to websocket
        sendMessage(jsonSubscribeMessage);
        
    }    


    // TODO - get this into postHandle interceptor.
    public String signObject(Subscribe jsonObj) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonObj);

        String timestamp = Instant.now().getEpochSecond() + "";
        jsonObj.setKey(key);
        jsonObj.setTimestamp(timestamp);
        jsonObj.setPassphrase(passphrase);
        jsonObj.setSignature(signature.generate("", "GET", jsonString, timestamp));

        return gson.toJson(jsonObj);
    }

    public <T> T getObject(String json, TypeReference<T> type) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * OrderBookMessage handler.
     *
     * @author Jiji_Sasidharan
     */
    public interface MessageHandler {
        public void handleMessage(String message);
    }
}
