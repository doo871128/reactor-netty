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

import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Log the http access information.
 * All logs are written to a Logger named {@code reactor.netty.http.server.AccessLog} at INFO level.
 * <p>
 * This class also exposes convenience methods to create an access log factory to be passed to {@link reactor.netty.http.server.HttpServer#accessLog(Function)}
 * during server configuration. Note that access logging must be globally enabled first for that configuration to
 * be taken into account, see the {@link reactor.netty.ReactorNetty#ACCESS_LOG_ENABLED} system property.
 *
 * @author limaoning
 */
public final class AccessLog {

	static final Logger LOG = Loggers.getLogger("reactor.netty.http.server.AccessLog");

	final String logFormat;
	final Object[] args;

	private AccessLog(String logFormat, Object... args) {
		Objects.requireNonNull(logFormat, "logFormat");
		this.logFormat = logFormat;
		this.args = args;
	}

	public static AccessLog create(String logFormat, Object... args) {
		return new AccessLog(logFormat, args);
	}

	/**
	 * Helper method to create an access log factory that selectively enables access logs.
	 * <p>
	 * Any request (represented as an {@link AccessLogArgProvider}) that doesn't match the
	 * provided {@link Predicate} is excluded from the access log. Other requests are logged
	 * using the default format.
	 *
	 * @param predicate the filter that returns {@code true} if the request should be logged, {@code false} otherwise
	 * @return an access log factory {@link Function} to be used in {@link reactor.netty.http.server.HttpServer#accessLog(Function)}
	 */
	public static Function<AccessLogArgProvider, AccessLog> filterFactory(Predicate<AccessLogArgProvider> predicate) {
		return input -> predicate.test(input) ? BaseAccessLogHandler.DEFAULT_ACCESS_LOG.apply(input) : null;
	}

	/**
	 * Helper method to create an access log factory that selectively enables access logs and customizes
	 * the format to apply.
	 * <p>
	 * Any request (represented as an {@link AccessLogArgProvider}) that doesn't match the
	 * provided {@link Predicate} is excluded from the access log. Other requests are logged
	 * using the provided formatting {@link Function}. Said function is expected to {@link AccessLog#create(String, Object...) create}
	 * an {@link AccessLog} instance, defining both the String format and a vararg of the relevant arguments, extracted from the
	 * {@link AccessLogArgProvider}.
	 * <p>
	 * Note that if filtering itself is not needed, one can directly provide such a formatting function
	 * to the {@link reactor.netty.http.server.HttpServer#accessLog(Function) HttpServer configuration}.
	 *
	 * @param predicate the filter that returns {@code true} if the request should be logged, {@code false} otherwise
	 * @param formatFunction the {@link Function} that creates {@link AccessLog} instances, encapsulating the format
	 * and the extraction of relevant arguments
	 * @return an access log factory {@link Function} to be used in {@link reactor.netty.http.server.HttpServer#accessLog(Function)}
	 */
	public static Function<AccessLogArgProvider, AccessLog> filterAndFormatFactory(Predicate<AccessLogArgProvider> predicate,
			Function<AccessLogArgProvider, AccessLog> formatFunction) {
		return input -> predicate.test(input) ? formatFunction.apply(input) : null;
	}

	void log() {
		if (LOG.isInfoEnabled()) {
			LOG.info(logFormat, args);
		}
	}

}
