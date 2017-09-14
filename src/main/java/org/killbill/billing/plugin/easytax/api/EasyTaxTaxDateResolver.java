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

import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfig;

/**
 * Resolver API for the effective tax date for an invoice item.
 * 
 * @author matt
 */
public interface EasyTaxTaxDateResolver {

    /**
     * Initialize the resolver for use.
     * 
     * <p>
     * This method is expected to be called <b>one time</b> immediately after an instance is
     * created.
     * </p>
     * 
     * @param killbillApi
     *            the API to use
     * @param config
     *            the configuration
     */
    void init(OSGIKillbill killbillApi, EasyTaxConfig config);

    /**
     * Resolve a tax date for an invoice item.
     * 
     * <p>
     * This method must be thread-safe.
     * </p>
     * 
     * @param kbTenantId
     *            the tenant ID
     * @param account
     *            the account
     * @param invoice
     *            the invoice
     * @param invoiceItem
     *            the invoice item
     * @param pluginProperties
     *            any available active plugin properties
     * @return the tax date, or {@literal null} if no date can be resolved
     */
    DateTime taxDateForInvoiceItem(UUID kbTenantId, Account account, Invoice invoice,
            InvoiceItem invoiceItem, Iterable<PluginProperty> pluginProperties);

}
