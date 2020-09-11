package nu.itark.frosk;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.PerConnectionWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import nu.itark.frosk.changedetection.CoinbaseWebSocketHandler;
import samples.websocket.tomcat.snake.SnakeWebSocketHandler;

@Configuration
@EnableAutoConfiguration
@EnableWebSocket
public class FroskWebSocketConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(snakeWebSocketHandler(), "/snake").withSockJS();
		registry.addHandler(coinbaseWebSocketHandler(), "/ws").withSockJS().setHttpMessageCacheSize(1000);

	}


	//Denna kanske håller kopplet vid liv.
    // @Bean
    // public ServletServerContainerFactoryBean createWebSocketContainer() {
    //     ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    //     container.setMaxTextMessageBufferSize(8192);
	// 	container.setMaxBinaryMessageBufferSize(8192);
    //     return container;
    // }


	@Bean
	public WebSocketHandler snakeWebSocketHandler() {
		return new PerConnectionWebSocketHandler(SnakeWebSocketHandler.class);
	}	

	
	@Bean
	public WebSocketHandler coinbaseWebSocketHandler() {
		return new PerConnectionWebSocketHandler(CoinbaseWebSocketHandler.class);
	}		
	
}
