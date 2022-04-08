package nu.itark.frosk;

import nu.itark.frosk.changedetection.CoinbaseWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import samples.websocket.tomcat.snake.SnakeWebSocketHandler;

import javax.servlet.ServletContext;

//@Configuration
//@EnableWebSocketMessageBroker
public class FroskWebSocketConfigBroker implements WebSocketMessageBrokerConfigurer {

	//alt 1. https://www.devglan.com/spring-boot/spring-boot-websocket-example
	//alt 2. https://www.baeldung.com/websockets-spring
	//alt 3. https://dzone.com/articles/full-duplex-scalable-client-server-communication-u
	//alt 4. https://spring.io/guides/gs/messaging-stomp-websocket/

	//nu alt 4

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/gs-guide-websocket").withSockJS();
		registry.addEndpoint("/websocket-broker").withSockJS();

	}



}
