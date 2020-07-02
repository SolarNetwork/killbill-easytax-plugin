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

import java.util.Optional;
import java.util.UUID;

import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.easytax.api.EasyTaxTaxZoneResolver;
import org.killbill.billing.plugin.easytax.api.EasyTaxTenantContext;
import org.killbill.billing.util.callcontext.TenantContext;

/**
 * Resolve tax zones based on a custom field on an account.
 * 
 * @author matt
 */
public class AccountCustomFieldTaxZoneResolver implements EasyTaxTaxZoneResolver {

    /** The configuration property for a boolean flag to use the account country. */
    // CHECKSTYLE OFF: LineLength
    public static final String USE_ACCOUNT_COUNTRY_PROPERTY = "accountCustomFieldTaxZoneResolver.useAccountCountry";
    // CHECKSTYLE ON: LineLength

    /** A custom field name that specifies the tax zone to apply. */
    public static final String TAX_ZONE_CUSTOM_FIIELD = "taxCode";

    private OSGIKillbill killbillApi;
    private EasyTaxConfig config;

    @Override
    public void init(OSGIKillbill killbillApi, EasyTaxConfig config) {
        this.killbillApi = killbillApi;
        this.config = config;
    }

    @Override
    public String taxZoneForInvoice(UUID kbTenantId, Account account, Invoice invoice,
            Iterable<PluginProperty> properties) {
        // look first for property-specified zone
        String taxZone = checkForTaxZoneInCustomFields(account,
                new EasyTaxTenantContext(kbTenantId, account.getId()));

        if (taxZone == null && isUseAccountCountryAsTaxZone() && account != null) {
            // fall back to account country
            taxZone = account.getCountry();
        }

        return taxZone;
    }

    /**
     * Look for a tax zone configured via custom fields, adding a {@link PluginProperty} for the
     * discovered tax zone, if found.
     * 
     * @param invoice
     *            the invoice to inspect
     * @param context
     *            the tenant context
     * @return the discovered tax zone, or {@literal null} if none found
     */
    private String checkForTaxZoneInCustomFields(final Account account,
            final TenantContext context) {
        final Optional<String> customFieldTaxZone = this.killbillApi.getCustomFieldUserApi()
                .getCustomFieldsForObject(account.getId(), ObjectType.ACCOUNT, context).stream()
                .filter(f -> TAX_ZONE_CUSTOM_FIIELD.equals(f.getFieldName())).findFirst()
                .map(f -> f.getFieldValue());
        return customFieldTaxZone.orElse(null);
    }

    /**
     * Get the flag to use the account's country as the tax zone if nothing more specific is
     * available.
     * 
     * <p>
     * This returns the {@link #USE_ACCOUNT_COUNTRY_PROPERTY}. Defaults to {@literal true}.
     * </p>
     * 
     * @return {@literal true} to use the account country as the tax zone
     */
    public boolean isUseAccountCountryAsTaxZone() {
        return "true".equalsIgnoreCase(
                config.getConfigurationValue(USE_ACCOUNT_COUNTRY_PROPERTY, "true"));
    }

}
