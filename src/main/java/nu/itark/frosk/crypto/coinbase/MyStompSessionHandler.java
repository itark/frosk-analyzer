package nu.itark.frosk.crypto.coinbase;

import java.lang.reflect.Type;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyStompSessionHandler  extends StompSessionHandlerAdapter{

	

	    @Override
	    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
	        log.info("New session established : " + session.getSessionId());
	        session.subscribe("/topic/messages", this);
	        log.info("Subscribed to /topic/messages");
	        session.send("/app/chat", getSampleMessage());
	        log.info("Message sent to websocket server");
	    }

	    @Override
	    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
	        log.error("Got an exception", exception);
	    }

	    @Override
	    public Type getPayloadType(StompHeaders headers) {
	        return Message.class;
	    }

	    @Override
	    public void handleFrame(StompHeaders headers, Object payload) {
	        Message msg = (Message) payload;
	        log.info("Received : " + msg.getText() + " from : " + msg.getFrom());
	    }

	    /**
	     * A sample message instance.
	     * @return instance of <code>Message</code>
	     */
	    private Message getSampleMessage() {
	        Message msg = new Message();
	        msg.setFrom("Nicky");
	        msg.setText("Howdy!!");
	        return msg;
	    }	
	
	
}
