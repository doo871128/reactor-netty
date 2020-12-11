/*
 * Copyright (c) 2011-Present VMware, Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.netty.http.server.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.netty.http.server.logging.LoggingTests.HEADER_CONNECTION_NAME;
import static reactor.netty.http.server.logging.LoggingTests.HEADER_CONNECTION_VALUE;
import static reactor.netty.http.server.logging.LoggingTests.RESPONSE_CONTENT;
import static reactor.netty.http.server.logging.LoggingTests.URI;

/**
 * @author limaoning
 */
class AccessLogHandlerH2Tests {

	@Test
	void filtering() {
		// given
		Function<AccessLogArgProvider, AccessLog> filteringFactory = AccessLog.filterFactory(p -> !String.valueOf(p.uri()).startsWith("/foo/"));

		// assert
		assertFilteringLog(filteringFactory, logLines -> assertThat(logLines)
				.hasSize(1)
				.allSatisfy(logLine -> assertThat(logLine)
						.contains("GET")
						.contains("HTTP/2.0")
						.contains("/example/bar")
						.doesNotContain("foo")));
	}

	@Test
	void filteringAndFormatting() {
		// given
		Function<AccessLogArgProvider, AccessLog> filteringFactory = AccessLog.filterAndFormatFactory(p -> !String.valueOf(p.uri()).startsWith("/foo/"),
				arg -> AccessLog.create("This is HTTP2 {}, uri={}", arg.method(), arg.uri()));

		// assert
		assertFilteringLog(filteringFactory, logLines -> assertThat(logLines)
				.containsExactly("[INFO] This is HTTP2 GET, uri=/example/bar"));
	}

	void assertFilteringLog(Function<AccessLogArgProvider, AccessLog> filteringFactory, Consumer<Stream<String>> assertions) {
		//given
		Http2Headers requestFiltered = new DefaultHttp2Headers().method(HttpMethod.GET.name()).path("/foo/example/bar");
		Http2Headers requestOk = new DefaultHttp2Headers().method(HttpMethod.GET.name()).path("/example/bar");

		// setup
		@SuppressWarnings("unchecked") Appender<ILoggingEvent> mockedAppender = (Appender<ILoggingEvent>) Mockito.mock(Appender.class);
		ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor = ArgumentCaptor.forClass(LoggingEvent.class);
		Mockito.when(mockedAppender.getName()).thenReturn("MOCK");
		Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		root.addAppender(mockedAppender);
		Http2Headers responseHeaders1 = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
		ByteBuf byteBuf1 = Unpooled.buffer(RESPONSE_CONTENT.length)
		                           .writeBytes(RESPONSE_CONTENT);
		Http2Headers responseHeaders2 = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
		ByteBuf byteBuf2 = Unpooled.buffer(RESPONSE_CONTENT.length).writeBytes(RESPONSE_CONTENT);

		try {
			EmbeddedChannel channel = new EmbeddedChannel();
			channel.pipeline().addLast(new AccessLogHandlerH2(filteringFactory));

			channel.writeInbound(new DefaultHttp2HeadersFrame(requestFiltered));
			channel.writeOutbound(new DefaultHttp2HeadersFrame(responseHeaders1));
			channel.writeOutbound(new DefaultHttp2DataFrame(byteBuf1, true));

			channel.writeInbound(new DefaultHttp2HeadersFrame(requestOk));
			channel.writeOutbound(new DefaultHttp2HeadersFrame(responseHeaders2));
			channel.writeOutbound(new DefaultHttp2DataFrame(byteBuf2, true));

			Mockito.verify(mockedAppender, Mockito.atLeastOnce()).doAppend(loggingEventArgumentCaptor.capture());

			assertions.accept(loggingEventArgumentCaptor.getAllValues().stream().map(String::valueOf));
		}
		finally {
			root.detachAppender(mockedAppender);
		}
	}

	@Test
	void accessLogArgs() {
		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(new AccessLogHandlerH2(
				args -> {
					assertAccessLogArgProvider(args, channel.remoteAddress());
					return AccessLog.create("{}={}", HEADER_CONNECTION_NAME,
							args.requestHeader(HEADER_CONNECTION_NAME));
				}));

		Http2Headers requestHeaders = new DefaultHttp2Headers();
		requestHeaders.method(HttpMethod.GET.name());
		requestHeaders.path(URI);
		requestHeaders.add(HEADER_CONNECTION_NAME, HEADER_CONNECTION_VALUE);
		channel.writeInbound(new DefaultHttp2HeadersFrame(requestHeaders));

		Http2Headers responseHeaders = new DefaultHttp2Headers();
		responseHeaders.status(HttpResponseStatus.OK.codeAsText());
		channel.writeOutbound(new DefaultHttp2HeadersFrame(responseHeaders));

		ByteBuf byteBuf = Unpooled.buffer(RESPONSE_CONTENT.length);
		byteBuf.writeBytes(RESPONSE_CONTENT);
		channel.writeOutbound(new DefaultHttp2DataFrame(byteBuf, true));
	}

	private void assertAccessLogArgProvider(AccessLogArgProvider args, SocketAddress remoteAddress) {
		assertThat(args.remoteAddress()).isEqualTo(remoteAddress);
		assertThat(args.user()).isEqualTo(AbstractAccessLogArgProvider.MISSING);
		assertThat(args.zonedDateTime()).isNotNull();
		assertThat(args.method()).isEqualTo(HttpMethod.GET.name());
		assertThat(args.uri()).isEqualTo(URI);
		assertThat(args.protocol()).isEqualTo(AccessLogArgProviderH2.H2_PROTOCOL_NAME);
		assertThat(args.status()).isEqualTo(HttpResponseStatus.OK.codeAsText());
		assertThat(args.contentLength()).isEqualTo(RESPONSE_CONTENT.length);
		assertThat(args.requestHeader(HEADER_CONNECTION_NAME)).isEqualTo(HEADER_CONNECTION_VALUE);
	}

}
