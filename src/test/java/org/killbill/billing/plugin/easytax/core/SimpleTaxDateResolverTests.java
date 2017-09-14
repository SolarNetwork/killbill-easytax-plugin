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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.plugin.easytax.EasyTaxTestUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the {@link SimpleTaxDateResolver} class.
 * 
 * @author matt
 */
public class SimpleTaxDateResolverTests {

    private static final String TEST_USAGE_PLAN_NAME = "test-data-usage";
    private static final LocalDate TEST_START_DATE = new LocalDate(2017, 8, 1);
    private static final LocalDate TEST_END_DATE = new LocalDate(2017, 8, 31);
    private static final LocalDate TEST_INVOICE_DATE = new LocalDate(2017, 9, 1);
    private static final BigDecimal TEST_USAGE_COST = new BigDecimal("9.99");

    private OSGIKillbill killbillApi;
    private UUID tenantId;
    private DateTime now;
    private Account account;
    private Invoice invoice;

    @BeforeMethod
    public void setup() {
        now = new DateTime();
        tenantId = UUID.randomUUID();
        killbillApi = Mockito.mock(OSGIKillbill.class);
    }

    @AfterTest
    public void teardown() {
        Mockito.verifyZeroInteractions(killbillApi);
        Mockito.validateMockitoUsage();
    }

    private List<InvoiceItem> setupDefaultInvoice(LocalDate invoiceDate, LocalDate startDate,
            LocalDate endDate) {
        return setupDefaultInvoice(now, now, invoiceDate, startDate, endDate);
    }

    private List<InvoiceItem> setupDefaultInvoice(DateTime invoiceCreatedDate,
            DateTime invoiceItemCreatedDate, LocalDate invoiceDate, LocalDate startDate,
            LocalDate endDate) {
        account = EasyTaxTestUtils.createAccount("NZ", DateTimeZone.forID("Pacific/Auckland"));
        List<InvoiceItem> items = new ArrayList<>();
        invoice = EasyTaxTestUtils.createInvoice(invoiceCreatedDate, invoiceDate, items);
        InvoiceItem usage = EasyTaxTestUtils.createInvoiceItem(account, invoice,
                InvoiceItemType.USAGE, TEST_USAGE_PLAN_NAME, invoiceItemCreatedDate, startDate,
                endDate, TEST_USAGE_COST, Currency.NZD);
        items.add(usage);
        return items;
    }

    private Properties resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode mode) {
        Properties props = new Properties();
        props.put(EasyTaxConfig.PROPERTY_PREFIX + SimpleTaxDateResolver.DATE_MODE_PROPERTY,
                mode.toPropertyValue());
        return props;
    }

    private SimpleTaxDateResolver createResolver(Properties properties) {
        SimpleTaxDateResolver resolver = new SimpleTaxDateResolver();
        resolver.init(killbillApi, new EasyTaxConfig(properties));
        return resolver;
    }

    private Properties configureFallbackToInvoiceDate(Properties properties, Boolean value) {
        properties.put(
                EasyTaxConfig.PROPERTY_PREFIX
                        + SimpleTaxDateResolver.FALLBACK_TO_INVOICE_DATE_PROPERTY,
                value == null ? null : value.toString());
        return properties;
    }

    private Properties configureFallbackToInvoiceItemCreatedDate(Properties properties,
            Boolean value) {
        properties.put(
                EasyTaxConfig.PROPERTY_PREFIX
                        + SimpleTaxDateResolver.FALLBACK_TO_INVOICE_ITEM_CREATED_DATE_PROPERTY,
                value == null ? null : value.toString());
        return properties;
    }

    private Properties configureFallbackToInvoiceCratedDate(Properties properties, Boolean value) {
        properties.put(
                EasyTaxConfig.PROPERTY_PREFIX
                        + SimpleTaxDateResolver.FALLBACK_TO_INVOICE_CREATED_DATE_PROPERTY,
                value == null ? null : value.toString());
        return properties;
    }

    @Test(groups = "fast")
    public void defaultUsageWithStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE,
                TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_END_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Default resolves end date");
    }

    @Test(groups = "fast")
    public void defaultUsageWithStartDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_START_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Default falls back to start date when end date missing");
    }

    @Test(groups = "fast")
    public void defaultUsageWithInvoiceDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Default falls back to invoice date when start/end dates mising");
    }

    @Test(groups = "fast")
    public void defaultUsageWithInvoiceItemCreatedDateFallback() {
        // given
        DateTime itemCreatedDate = new DateTime();
        List<InvoiceItem> items = setupDefaultInvoice(now, itemCreatedDate, null, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertSame(result, itemCreatedDate,
                "Default resolves item created date when start/end/invoice dates mising");
    }

    @Test(groups = "fast")
    public void defaultUsageWithInvoiceCreatedDateFallback() {
        // given
        DateTime invoiceCreatedDate = new DateTime();
        List<InvoiceItem> items = setupDefaultInvoice(invoiceCreatedDate, null, null, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertSame(result, invoiceCreatedDate,
                "Default resolves null when start/end/invoice/item created dates mising");
    }

    @Test(groups = "fast")
    public void defaultUsageWithNullFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(null, null, null, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(new Properties());
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertNull(result,
                "Default resolves null when start/end/invoice/item created/invoice created dates mising");
    }

    @Test(groups = "fast")
    public void endDateModeUsageWithStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE,
                TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.End));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_END_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "End mode resolves end date");
    }

    @Test(groups = "fast")
    public void endDateModeUsageWithInvoiceDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.End));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "End DateMode falls back to invoice date when missing end date");
    }

    @Test(groups = "fast")
    public void startDateModeUsageWithStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE,
                TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.Start));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_START_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Start mode resolves start date");
    }

    @Test(groups = "fast")
    public void startDateModeUsageWithInvoiceDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, null, TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.Start));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Start DateMode falls back to invoice date when missing start date");
    }

    @Test(groups = "fast")
    public void startThenEndDateModeUsageWithStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE,
                TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.StartThenEnd));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_START_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "StartThenEnd mode resolves start date");
    }

    @Test(groups = "fast")
    public void startThenEndDateModeUsageWithEndDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, null, TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.StartThenEnd));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_END_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "StartThenEnd mode falls back to end date when missing start date");
    }

    @Test(groups = "fast")
    public void startThenEndDateModeUsageWithInvoiceDateFallback() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.StartThenEnd));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "StartThenEnd mode falls back to invoice date when start/end dates mising");
    }

    @Test(groups = "fast")
    public void invoiceDateModeUsageWithStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, TEST_START_DATE,
                TEST_END_DATE);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.Invoice));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Invoice mode resolves invoice date");
    }

    @Test(groups = "fast")
    public void invoiceDateModeUsageWithMissingStartEndDates() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(TEST_INVOICE_DATE, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                resolverPropertiesForDateMode(SimpleTaxDateResolver.DateMode.Invoice));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertEquals(result, TEST_INVOICE_DATE.toDateTimeAtStartOfDay(account.getTimeZone()),
                "Invoice mode resolves invoice date when missing start/end dates");
    }

    @Test(groups = "fast")
    public void noInvoiceDateFallbackUsage() {
        // given
        final DateTime invoiceItemCreatedDate = new DateTime();
        List<InvoiceItem> items = setupDefaultInvoice(now, invoiceItemCreatedDate,
                TEST_INVOICE_DATE, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                configureFallbackToInvoiceDate(new Properties(), false));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertSame(result, invoiceItemCreatedDate,
                "Disabling invoice date fallback resolves invoice item created date");
    }

    @Test(groups = "fast")
    public void noInvoiceItemCreatedDateFallbackUsage() {
        // given
        final DateTime invoiceCreatedDate = new DateTime();
        List<InvoiceItem> items = setupDefaultInvoice(invoiceCreatedDate, now, TEST_INVOICE_DATE,
                null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(configureFallbackToInvoiceItemCreatedDate(
                configureFallbackToInvoiceDate(new Properties(), false), false));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertSame(result, invoiceCreatedDate,
                "Disabling invoice item created date fallback resolves invoice created date");
    }

    @Test(groups = "fast")
    public void noInvoiceCreatedDateFallbackUsage() {
        // given
        List<InvoiceItem> items = setupDefaultInvoice(now, now, TEST_INVOICE_DATE, null, null);

        // when
        SimpleTaxDateResolver resolver = createResolver(
                configureFallbackToInvoiceCratedDate(
                        configureFallbackToInvoiceItemCreatedDate(
                                configureFallbackToInvoiceDate(new Properties(), false), false),
                        false));
        DateTime result = resolver.taxDateForInvoiceItem(tenantId, account, invoice, items.get(0),
                Collections.emptyList());

        // then
        Assert.assertNull(result, "Disabling invoice item created date fallback resolves null");
    }

}
