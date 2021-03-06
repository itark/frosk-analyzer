/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.itark.frosk.changedetection;

import lombok.extern.slf4j.Slf4j;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Channels;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import nu.itark.frosk.coinbase.exchange.api.websocketfeed.Observations;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.WebsocketFeed;
import nu.itark.frosk.coinbase.exchange.api.websocketfeed.message.Subscribe;

@Slf4j
public class CoinbaseWebSocketHandler extends TextWebSocketHandler {
	
	private static final Log logger = LogFactory.getLog(CoinbaseWebSocketHandler.class);

	@Autowired
	WebsocketFeed websocketFeed;


	@Autowired
	Observations observations;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		logger.trace("::afterConnectionEstablished::");
		websocketFeed.subscribe(getSubscribe(Subscribe.FULL));
		observations.setWebSocketsession(session);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws Exception {
//		logger.info("::handleTextMessage::message: "+message);

	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
			throws Exception {
		log.info("::afterConnectionClosed:: session{}: status{}, ",session,status);

		//TODO stäng koppel till coinbase
		websocketFeed.unsubscribe(getSubscribe(Subscribe.FULL));
	}


	private static Subscribe getSubscribe(String subchannel) {
		String[] productIds = new String[]{"BTC-EUR"}; // make this configurable.
		Channels[] channel = new Channels[1];
		channel[0] = new Channels();
		channel[0].setName(subchannel);
		channel[0].setProduct_ids(productIds);

		return new Subscribe(channel);

	}

	
}
