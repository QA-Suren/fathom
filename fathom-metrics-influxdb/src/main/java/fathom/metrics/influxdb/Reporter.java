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
package fathom.metrics.influxdb;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import fathom.conf.Settings;
import fathom.metrics.MetricsReporter;
import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Integration of Fathom Metrics with InfluxDB.
 *
 * @author James Moger
 */
public class Reporter implements MetricsReporter {

    private final Logger log = LoggerFactory.getLogger(Reporter.class);

    private InfluxdbReporter reporter;

    @Override
    public void start(Settings settings, MetricRegistry metricRegistry) {
        if (settings.getBoolean("metrics.influxdb.enabled", false)) {

            final String hostname = settings.getLocalHostname();
            final String address = settings.getRequiredString("metrics.influxdb.address");
            final int port = settings.getInteger("metrics.influxdb.port", 8086);
            final String database = settings.getRequiredString("metrics.influxdb.database");
            final String username = settings.getRequiredString("metrics.influxdb.username");
            final String password = settings.getRequiredString("metrics.influxdb.password");
            final long period = settings.getDuration("metrics.influxdb.period", TimeUnit.SECONDS, 60);

            try {

                Influxdb influxdb = new Influxdb(address, port, database, username, password);
                reporter = InfluxdbReporter.forRegistry(metricRegistry).prefixedWith(hostname)
                        .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
                        .filter(MetricFilter.ALL).build(influxdb);

                reporter.start(period, TimeUnit.SECONDS);

                log.debug("Started InfluxDB Metrics reporter for '{}', updating every {} seconds", hostname, period);

            } catch (Exception e) {
                log.error("Failed to start InfluxDB reporter!", e);
            }
        } else {
            log.debug("InfluxDB Metrics reporter is disabled");
        }
    }

    @Override
    public void close() throws IOException {
        if (reporter != null) {

            reporter.stop();

            log.debug("Stopped InfluxDB Metrics reporter");
        }
    }
}
