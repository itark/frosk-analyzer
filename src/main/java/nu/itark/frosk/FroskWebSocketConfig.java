package nu.itark.frosk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;

import nu.itark.frosk.changedetection.CoinbaseWebSocketHandler;
import samples.websocket.tomcat.snake.SnakeWebSocketHandler;

import javax.servlet.ServletContext;

@Configuration
@EnableAutoConfiguration
@EnableWebSocket
public class FroskWebSocketConfig  implements WebSocketConfigurer {

	//alt 1. https://www.devglan.com/spring-boot/spring-boot-websocket-example
	//alt 2. https://www.baeldung.com/websockets-spring
	//alt 3. https://dzone.com/articles/full-duplex-scalable-client-server-communication-u


	@Autowired
	ServletContext servletContext;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(snakeWebSocketHandler(), "/snake").withSockJS();
		registry.addHandler(coinbaseWebSocketHandler(), "/ws").withSockJS();
		registry.addHandler(coinbaseWebSocketHandler(), "/frosk-analyzer/ws").withSockJS();

				//.setHttpMessageCacheSize(1000);
	}

	@Bean
	public WebSocketHandler snakeWebSocketHandler() {
		return new PerConnectionWebSocketHandler(SnakeWebSocketHandler.class);
	}


//	@Bean
//	public DefaultHandshakeHandler handshakeHandler() {
//
//		WebSocketPolicy policy = new WebSocketPolicy(WebSocketBehavior.SERVER);
//		policy.setInputBufferSize(8192);
//		policy.setIdleTimeout(600000);
//
//		return new DefaultHandshakeHandler(
//				new JettyRequestUpgradeStrategy(new WebSocketServerFactory(servletContext, policy)));
//	}




//	http://www.dineshsawant.com/jetty-websocket-with-spring-boot/

	@Bean
	public WebSocketHandler coinbaseWebSocketHandler() {
		return new PerConnectionWebSocketHandler(CoinbaseWebSocketHandler.class);
	}		
	
}
