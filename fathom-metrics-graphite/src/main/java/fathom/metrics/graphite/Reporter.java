/*
 * Copyright (C) 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fathom.metrics.graphite;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.PickledGraphite;
import fathom.conf.Settings;
import fathom.metrics.MetricsReporter;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Integration of Fathom Metrics with Graphite.
 *
 * @author James Moger
 */
@MetaInfServices
public class Reporter implements MetricsReporter {

    private final Logger log = LoggerFactory.getLogger(Reporter.class);

    private GraphiteReporter reporter;

    @Override
    public void start(Settings settings, MetricRegistry metricRegistry) {
        if (settings.getBoolean("metrics.graphite.enabled", false)) {

            final String hostname = settings.getLocalHostname();
            final String address = settings.getRequiredString("metrics.graphite.address");
            final int port = settings.getInteger("metrics.graphite.port", 2003);
            final boolean isPickled = settings.getBoolean("metrics.graphite.pickled", false);
            final long period = settings.getDuration("metrics.graphite.period", TimeUnit.SECONDS, 60);

            final InetSocketAddress graphiteAddress = new InetSocketAddress(address, port);

            final GraphiteSender sender;
            if (isPickled) {
                sender = new PickledGraphite(graphiteAddress);
            } else {
                sender = new Graphite(graphiteAddress);
            }

            reporter = GraphiteReporter.forRegistry(metricRegistry).prefixedWith(hostname)
                    .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
                    .filter(MetricFilter.ALL).build(sender);

            reporter.start(period, TimeUnit.SECONDS);

            log.debug("Started Graphite Metrics reporter for '{}', updating every {} seconds", hostname, period);

        } else {
            log.debug("Graphite Metrics reporter is disabled");
        }
    }

    @Override
    public void close() throws IOException {
        if (reporter != null) {

            reporter.stop();

            log.debug("Stopped Graphite Metrics reporter");
        }
    }
}
