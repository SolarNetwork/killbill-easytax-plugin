/*  Copyright 2017 SolarNetwork Foundation
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.killbill.billing.plugin.easytax.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.killbill.billing.invoice.plugin.api.InvoicePluginApi;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.billing.plugin.api.notification.PluginConfigurationEventHandler;
import org.killbill.billing.plugin.easytax.api.EasyTaxInvoicePluginApi;
import org.killbill.billing.plugin.easytax.api.EasyTaxTaxDateResolver;
import org.killbill.billing.plugin.easytax.api.EasyTaxTaxZoneResolver;
import org.killbill.billing.plugin.easytax.dao.JooqEasyTaxDao;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.osgi.framework.BundleContext;

public class EasyTaxActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-easytax";

    private EasyTaxConfigurationHandler configurationHandler;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final DataSource ds = dataSource.getDataSource();
        final SQLDialect dialect = detectSqlDialect(ds);
        final JooqEasyTaxDao dao = new JooqEasyTaxDao(ds, dialect);
        final Clock clock = new DefaultClock();

        configurationHandler = new EasyTaxConfigurationHandler(PLUGIN_NAME, killbillAPI,
                logService);

        final EasyTaxConfig globalConfig = configurationHandler
                .createConfigurable(configProperties.getProperties());
        configurationHandler.setDefaultConfigurable(globalConfig);

        ServiceResolver<EasyTaxTaxZoneResolver> taxZoneResolverService = new ServiceResolver<>(
                context, EasyTaxTaxZoneResolver.class, null);
        ServiceResolver<EasyTaxTaxDateResolver> taxDateResolverService = new ServiceResolver<>(
                context, EasyTaxTaxDateResolver.class, null);

        final InvoicePluginApi invoicePluginApi = new EasyTaxInvoicePluginApi(configurationHandler,
                dao, taxZoneResolverService, taxDateResolverService, killbillAPI, configProperties,
                logService, clock);
        registerInvoicePluginApi(context, invoicePluginApi);

        final HttpServlet servlet = new EasyTaxServlet(dao, clock);
        registerServlet(context, servlet);

        registerEventHandler();
    }

    private void registerEventHandler() {
        final PluginConfigurationEventHandler handler = new PluginConfigurationEventHandler(
                configurationHandler);
        dispatcher.registerEventHandlers(handler);
    }

    private void registerInvoicePluginApi(final BundleContext context, final InvoicePluginApi api) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, InvoicePluginApi.class, api, props);
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }

    private SQLDialect detectSqlDialect(DataSource dataSource) throws SQLException {
        String databaseProductName;
        try (Connection conn = dataSource.getConnection()) {
            databaseProductName = conn.getMetaData().getDatabaseProductName();
        }

        if ("H2".equalsIgnoreCase(databaseProductName)) {
            return SQLDialect.H2;
        } else if ("MySQL".equalsIgnoreCase(databaseProductName)) {
            return SQLDialect.MARIADB;
        } else if ("PostgreSQL".equalsIgnoreCase(databaseProductName)) {
            return SQLDialect.POSTGRES;
        }
        throw new IllegalArgumentException("Unknown DB dialect: " + databaseProductName);
    }

}
