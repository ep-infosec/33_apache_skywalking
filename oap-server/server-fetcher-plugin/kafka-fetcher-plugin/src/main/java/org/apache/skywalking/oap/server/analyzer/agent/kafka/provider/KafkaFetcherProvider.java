/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.analyzer.agent.kafka.provider;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.log.analyzer.module.LogAnalyzerModule;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.KafkaFetcherHandlerRegister;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.module.KafkaFetcherConfig;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.module.KafkaFetcherModule;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.JVMMetricsHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.JsonLogHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.LogHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.MeterServiceHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.ProfileTaskHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.ServiceManagementHandler;
import org.apache.skywalking.oap.server.analyzer.agent.kafka.provider.handler.TraceSegmentHandler;
import org.apache.skywalking.oap.server.analyzer.module.AnalyzerModule;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;
import org.apache.skywalking.oap.server.library.module.ModuleProvider;
import org.apache.skywalking.oap.server.library.module.ModuleStartException;
import org.apache.skywalking.oap.server.library.module.ServiceNotProvidedException;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;

@Slf4j
public class KafkaFetcherProvider extends ModuleProvider {
    private KafkaFetcherHandlerRegister handlerRegister;
    private KafkaFetcherConfig config;

    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return KafkaFetcherModule.class;
    }

    @Override
    public ConfigCreator newConfigCreator() {
        return new ConfigCreator<KafkaFetcherConfig>() {
            @Override
            public Class type() {
                return KafkaFetcherConfig.class;
            }

            @Override
            public void onInitialized(final KafkaFetcherConfig initialized) {
                config = initialized;
            }
        };
    }

    @Override
    public void prepare() throws ServiceNotProvidedException {
        handlerRegister = new KafkaFetcherHandlerRegister(config);
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {
        handlerRegister.register(new JVMMetricsHandler(getManager(), config));
        handlerRegister.register(new ServiceManagementHandler(getManager(), config));
        handlerRegister.register(new TraceSegmentHandler(getManager(), config));
        handlerRegister.register(new ProfileTaskHandler(getManager(), config));
        handlerRegister.register(new MeterServiceHandler(getManager(), config));

        if (config.isEnableNativeProtoLog()) {
            handlerRegister.register(new LogHandler(getManager(), config));
        }
        if (config.isEnableNativeJsonLog()) {
            handlerRegister.register(new JsonLogHandler(getManager(), config));
        }

        handlerRegister.start();
    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException {
    }

    @Override
    public String[] requiredModules() {
        return new String[] {
            TelemetryModule.NAME,
            AnalyzerModule.NAME,
            LogAnalyzerModule.NAME,
            CoreModule.NAME
        };
    }

}