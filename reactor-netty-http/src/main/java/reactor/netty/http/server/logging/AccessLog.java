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

/**
 * Log the http access information.
 * All logs are written to a Logger named {@code reactor.netty.http.server.AccessLog} at INFO level.
 * <p>
 * See {@link AccessLogFactory} for convenience methods to create an access log factory to be passed to
 * {@link reactor.netty.http.server.HttpServer#accessLog(boolean, AccessLogFactory)} during server configuration.
 * Note that access logging must be globally enabled first for that configuration to
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

	void log() {
		if (LOG.isInfoEnabled()) {
			LOG.info(logFormat, args);
		}
	}

}
