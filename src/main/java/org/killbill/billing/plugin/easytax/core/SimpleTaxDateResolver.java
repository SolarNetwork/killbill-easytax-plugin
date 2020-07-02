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

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.easytax.api.EasyTaxTaxDateResolver;

/**
 * Simple implementation of {@link EasyTaxTaxDateResolver} that can use either the end or start date
 * of an invoice item.
 * 
 * <p>
 * If neither a start nor end date are available, this class will fall back to the invoice date.
 * </p>
 * 
 * @author matt
 */
public class SimpleTaxDateResolver implements EasyTaxTaxDateResolver {

    /**
     * The configuration property key for the date mode to use.
     * 
     * <p>
     * The property value is expected to be in {@link DateMode#toPropertyValue()} form.
     * </p>
     */
    public static final String DATE_MODE_PROPERTY = "simpleTaxDateResolver.dateMode";

    /**
     * The configuration property key for the flag to fall back to the invoice date if needed.
     * 
     * <p>
     * The property value is expected to be {@literal true} or {@literal false}.
     * </p>
     */
    // CHECKSTYLE OFF: LineLength
    public static final String FALLBACK_TO_INVOICE_DATE_PROPERTY = "simpleTaxDateResolver.fallBackToInvoiceDate";
    // CHECKSTYLE ON: LineLength

    /**
     * The configuration property key for the flag to fall back to the invoice item created date if
     * needed.
     * 
     * <p>
     * The property value is expected to be {@literal true} or {@literal false}.
     * </p>
     */
    // CHECKSTYLE OFF: LineLength
    public static final String FALLBACK_TO_INVOICE_ITEM_CREATED_DATE_PROPERTY = "simpleTaxDateResolver.fallBackToInvoiceItemCreatedDate";
    // CHECKSTYLE ON: LineLength

    /**
     * The configuration property key for the flag to fall back to the invoice created date if
     * needed.
     * 
     * <p>
     * The property value is expected to be {@literal true} or {@literal false}.
     * </p>
     */
    // CHECKSTYLE OFF: LineLength
    public static final String FALLBACK_TO_INVOICE_CREATED_DATE_PROPERTY = "simpleTaxDateResolver.fallBackToInvoiceCreatedDate";
    // CHECKSTYLE ON: LineLength

    /**
     * The configuration property key for the time zone ID to use if the account does not have one.
     */
    public static final String DEFAULT_TZ_PROPERTY = "simpleTaxDateResolver.defaultTimeZone";

    /**
     * The supported date modes.
     */
    public enum DateMode {
        Invoice, Start, StartThenEnd, End, EndThenStart;

        /**
         * Get a property key value.
         * 
         * @return the property value for this mode.
         */
        public String toPropertyValue() {
            return toString().toLowerCase();
        }

        /**
         * Get a value from a string property value.
         * 
         * <p>
         * This will always return a value, falling back to {@code EndThenStart} if the value is
         * unknown.
         * </p>
         * 
         * @param value
         *            the value
         * @return the enum, never {@literal null}
         */
        public static DateMode fromPropertyValue(String value) {
            String lcValue = (value == null ? "" : value.toLowerCase());
            switch (lcValue) {
                case "invoice":
                    return DateMode.Invoice;

                case "start":
                    return DateMode.Start;

                case "startthenend":
                    return DateMode.StartThenEnd;

                case "end":
                    return DateMode.End;

                default:
                    return DateMode.EndThenStart;
            }
        }
    }

    private DateMode mode = DateMode.EndThenStart;
    private boolean fallBackToInvoiceDate = true;
    private boolean fallBackToInvoiceItemCreatedDate = true;
    private boolean fallBackToInvoiceCreatedDate = true;
    private DateTimeZone defaultTimeZone = DateTimeZone.UTC;

    @Override
    public void init(OSGIKillbill killbillApi, EasyTaxConfig config) {
        if (config != null) {
            mode = DateMode
                    .fromPropertyValue(config.getConfigurationValue(DATE_MODE_PROPERTY, null));
            fallBackToInvoiceDate = Boolean.valueOf(
                    config.getConfigurationValue(FALLBACK_TO_INVOICE_DATE_PROPERTY, "true"));
            fallBackToInvoiceItemCreatedDate = Boolean.valueOf(config
                    .getConfigurationValue(FALLBACK_TO_INVOICE_ITEM_CREATED_DATE_PROPERTY, "true"));
            fallBackToInvoiceCreatedDate = Boolean.valueOf(config
                    .getConfigurationValue(FALLBACK_TO_INVOICE_CREATED_DATE_PROPERTY, "true"));
            defaultTimeZone = DateTimeZone
                    .forID(config.getConfigurationValue(DEFAULT_TZ_PROPERTY, "UTC"));
        }
    }

    @Override
    public DateTime taxDateForInvoiceItem(UUID kbTenantId, Account account, Invoice invoice,
            InvoiceItem item, Iterable<PluginProperty> pluginProperties) {
        LocalDate applicableDate = null;
        switch (mode) {
            case Invoice:
                applicableDate = invoice.getInvoiceDate();
                break;

            case Start:
            case StartThenEnd:
                applicableDate = item.getStartDate();
                if (applicableDate == null && mode == DateMode.StartThenEnd) {
                    applicableDate = item.getEndDate();
                }
                break;

            default:
                applicableDate = item.getEndDate();
                if (applicableDate == null && mode == DateMode.EndThenStart) {
                    applicableDate = item.getStartDate();
                }
        }

        if (applicableDate == null && fallBackToInvoiceDate) {
            applicableDate = invoice.getInvoiceDate();
        }
        if (applicableDate != null) {
            DateTimeZone taxationTimeZone = account.getTimeZone();
            if (taxationTimeZone == null) {
                taxationTimeZone = defaultTimeZone;
            }
            return applicableDate.toDateTimeAtStartOfDay(taxationTimeZone);
        }
        // use item or invoice creation dates
        if (fallBackToInvoiceItemCreatedDate && item.getCreatedDate() != null) {
            return item.getCreatedDate();
        }
        if (fallBackToInvoiceCreatedDate && invoice.getCreatedDate() != null) {
            return invoice.getCreatedDate();
        }
        return null;
    }

}
