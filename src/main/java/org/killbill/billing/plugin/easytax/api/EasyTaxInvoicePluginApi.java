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

package org.killbill.billing.plugin.easytax.api;

import java.util.List;

import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginInvoicePluginApi;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfigurationHandler;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.clock.Clock;

/**
 * Main plugin entry point.
 * 
 * @author matt
 */
@SuppressWarnings("deprecation")
public class EasyTaxInvoicePluginApi extends PluginInvoicePluginApi {

    private final EasyTaxTaxCalculator calculator;

    /**
     * Constructor.
     * 
     * @param configurationHandler
     *            the configuration handler to use
     * @param dao
     *            the DAO to use for persistence
     * @param taxZoneResolver
     *            the tax zone resolver service
     * @param taxDateResolver
     *            the tax date resolver service
     * @param killbillApi
     *            the API to use
     * @param configProperties
     *            the configuration properties to use
     * @param logService
     *            the log service to use
     * @param clock
     *            the system clock
     */
    public EasyTaxInvoicePluginApi(final EasyTaxConfigurationHandler configurationHandler,
            final EasyTaxDao dao, final OptionalService<EasyTaxTaxZoneResolver> taxZoneResolver,
            final OptionalService<EasyTaxTaxDateResolver> taxDateResolver,
            final OSGIKillbillAPI killbillApi, final OSGIConfigPropertiesService configProperties,
            final OSGIKillbillLogService logService, final Clock clock) {
        super(killbillApi, configProperties, logService, clock);
        this.calculator = new EasyTaxTaxCalculator(killbillApi, configurationHandler, dao,
                taxZoneResolver, taxDateResolver, clock);
    }

    @Override
    public List<InvoiceItem> getAdditionalInvoiceItems(final Invoice invoice, final boolean dryRun,
            final Iterable<PluginProperty> properties, final CallContext context) {
        return getAdditionalTaxInvoiceItems(calculator, invoice, dryRun, properties, context);
    }

}
