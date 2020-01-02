/**
 * Copyright 2015 Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.ranger.core.serviceprovider;

import com.flipkart.ranger.core.model.NodeDataSource;
import com.flipkart.ranger.core.model.Service;
import com.flipkart.ranger.core.signals.ExternalTriggeredSignal;
import com.flipkart.ranger.core.signals.Signal;
import com.flipkart.ranger.core.healthcheck.HealthcheckResult;
import com.flipkart.ranger.core.model.ServiceNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ServiceProvider<T> {

    private final Service service;
    private final ServiceNode<T> serviceNode;
    private final NodeDataSource<T> dataSource;
    @Getter
    private final ExternalTriggeredSignal<Void> startSignal = new ExternalTriggeredSignal<>(() -> null, Collections.emptyList());
    @Getter
    private final ExternalTriggeredSignal<Void> stopSignal = new ExternalTriggeredSignal<>(() -> null, Collections.emptyList());

    public ServiceProvider(
            Service service,
            ServiceNode<T> serviceNode,
            NodeDataSource<T> dataSource,
            List<Signal<HealthcheckResult>> signalGenerators) {
        this.service = service;
        this.serviceNode = serviceNode;
        this.dataSource = dataSource;
        signalGenerators.forEach(signalGenerator -> signalGenerator.registerConsumer(this::handleHealthUpdate));
    }

    public void start() {
        startSignal.trigger();
        dataSource.updateState(serviceNode);
        log.debug("Set initial node data on zookeeper for {}", service.getServiceName());
    }

    public void stop() {
        stopSignal.trigger();
    }

    private void handleHealthUpdate(HealthcheckResult result) {
        if(null == result) {
            log.debug("No update to health state of node. Skipping data source update.");
            return;
        }
        serviceNode.setHealthcheckStatus(result.getStatus());
        serviceNode.setLastUpdatedTimeStamp(result.getUpdatedTime());
        dataSource.updateState(serviceNode);
        log.debug("Updated node with health check result: {}", result);
    }

}