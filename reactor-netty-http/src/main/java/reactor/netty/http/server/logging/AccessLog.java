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
 *
 * @author limaoning
 */
public final class AccessLog {

	static final Logger LOG = Loggers.getLogger("reactor.netty.http.server.AccessLog");

	final Logger logger;
	final String logFormat;
	final Object[] args;

	private AccessLog(Logger logger, String logFormat, Object... args) {
		Objects.requireNonNull(logFormat, "logFormat");
		this.logger = logger;
		this.logFormat = logFormat;
		this.args = args;
	}

	public static AccessLog create(String logFormat, Object... args) {
		return new AccessLog(LOG, logFormat, args);
	}

	public static AccessLog createWithLogger(Logger logger, String logFormat, Object... args) {
		return new AccessLog(logger, logFormat, args);
	}

	public static Function<AccessLogArgProvider, AccessLog> defaultFactory(Logger logger) {
		return BaseAccessLogHandler.DEFAULT_ACCESS_LOG;
	}

	public static Function<AccessLogArgProvider, AccessLog> filterFactory(Predicate<AccessLogArgProvider> predicate) {
		return accessLogArgProvider -> predicate.test(accessLogArgProvider) ? BaseAccessLogHandler.DEFAULT_ACCESS_LOG.apply(accessLogArgProvider) : null;
	}

	public static Function<AccessLogArgProvider, AccessLog> withLogger(Logger logger, Function<AccessLogArgProvider, AccessLog> f){
		return input -> {
			AccessLog accessLog = f.apply(input);
			return accessLog != null ? AccessLog.createWithLogger(logger, accessLog.logFormat, accessLog.args) : null;
		} ;
	}

	void log() {
		if (logger.isInfoEnabled()) {
			logger.info(logFormat, args);
		}
	}

}
