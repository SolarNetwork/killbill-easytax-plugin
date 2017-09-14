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

package org.killbill.billing.plugin.easytax;

import static org.mockito.Answers.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.plugin.easytax.api.OptionalService;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Test utilities.
 * 
 * @author matt
 */
public final class EasyTaxTestUtils {

    private EasyTaxTestUtils() {
        // don't create me
    }

    /**
     * This method compares two {@link DateTime} objects, ignoring their time zones.
     * 
     * @param actual
     *            the actual value
     * @param expected
     *            the expected value
     * @param msg
     *            a message to use
     */
    public static void assertDateTimeEquals(DateTime actual, DateTime expected, String msg) {
        if (!actual.getZone().equals(expected.getZone())) {
            actual = actual.withZone(expected.getZone());
        }
        assertEquals(actual, expected, msg);
    }

    public static Account createAccount(String country, DateTimeZone timeZone) {
        UUID id = UUID.randomUUID();
        Account account = Mockito.mock(Account.class,
                Mockito.withSettings().defaultAnswer(RETURNS_SMART_NULLS.get()));
        when(account.getId()).thenReturn(id);
        when(account.getCountry()).thenReturn(country);
        when(account.getTimeZone()).thenReturn(timeZone);
        return account;
    }

    public static Invoice createInvoice(DateTime createdDate, LocalDate invoiceDate,
            List<InvoiceItem> invoiceItems) {
        UUID id = UUID.randomUUID();
        Invoice invoice = Mockito.mock(Invoice.class,
                Mockito.withSettings().defaultAnswer(RETURNS_SMART_NULLS.get()));
        when(invoice.getId()).thenReturn(id);
        when(invoice.getCreatedDate()).thenReturn(createdDate);
        when(invoice.getInvoiceDate()).thenReturn(invoiceDate);
        when(invoice.getInvoiceItems()).thenReturn(invoiceItems);
        return invoice;
    }

    public static InvoiceItem createInvoiceItem(Account account, Invoice invoice,
            InvoiceItemType type, String planName, DateTime createdDate, LocalDate startDate,
            LocalDate endDate, BigDecimal amount, Currency currency) {
        UUID id = UUID.randomUUID();
        UUID accountId = account.getId();
        UUID invoiceId = invoice.getId();
        InvoiceItem item = Mockito.mock(InvoiceItem.class,
                Mockito.withSettings().defaultAnswer(RETURNS_SMART_NULLS.get()));
        when(item.getId()).thenReturn(id);
        when(item.getAccountId()).thenReturn(accountId);
        when(item.getCreatedDate()).thenReturn(createdDate);
        when(item.getCurrency()).thenReturn(currency);
        when(item.getAmount()).thenReturn(amount);
        when(item.getInvoiceId()).thenReturn(invoiceId);
        when(item.getInvoiceItemType()).thenReturn(type);
        when(item.getPlanName()).thenReturn(planName);
        when(item.getStartDate()).thenReturn(startDate);
        when(item.getEndDate()).thenReturn(endDate);
        return item;
    }

    /**
     * Create an {@link OptionalService} mock for a given service instance.
     * 
     * @param service
     *            the instance to be returned by {@link OptionalService#service(String)}
     * @param filterCaptor
     *            an optional captor for the filter argument passed to
     *            {@link OptionalService#service(String)}
     * @return the new optional service mock instance
     */
    public static <T> OptionalService<T> createOptionalService(T service,
            ArgumentCaptor<String> filterCaptor) {
        @SuppressWarnings("unchecked")
        OptionalService<T> resolver = Mockito.mock(OptionalService.class);
        if (filterCaptor != null) {
            when(resolver.service(filterCaptor.capture())).thenReturn(service);
        } else {
            when(resolver.service(Mockito.anyString())).thenReturn(service);
        }
        return resolver;
    }

    /**
     * Assert two {@link BigDecimal} values are equal at a given scale.
     * 
     * @param actual
     *            the actual value
     * @param expected
     *            the expected value
     * @param scale
     *            the comparison scale
     * @param msg
     *            a message
     */
    public static void assertBigDecimalEquals(BigDecimal actual, BigDecimal expected, int scale,
            String msg) {
        assertEquals(roundedAmount(actual, scale), roundedAmount(expected, scale), msg);
    }

    /**
     * Round (scale) a {@link BigDecimal}.
     * 
     * @param amount
     *            the value to round
     * @param scale
     *            the number of decimal places to round to (half up)
     * @return the rounded value
     */
    public static BigDecimal roundedAmount(BigDecimal amount, int scale) {
        return amount != null ? amount.setScale(scale, RoundingMode.HALF_UP) : null;
    }

    /**
     * Assert two {@link EasyTaxTaxation} instance have the same values.
     * 
     * @param actual
     *            the actual value
     * @param expected
     *            the expected value
     * @param msg
     *            a message
     */
    public static void assertEquivalent(EasyTaxTaxation actual, EasyTaxTaxation expected,
            String msg) {
        assertEquals(actual.getCreatedDate(), expected.getCreatedDate(), msg + " Created date");
        assertEquals(actual.getInvoiceItemIds(), expected.getInvoiceItemIds(),
                msg + " Invoice item IDs");
        assertEquals(actual.getKbAccountId(), expected.getKbAccountId(), msg + " Account ID");
        assertEquals(actual.getKbInvoiceId(), expected.getKbInvoiceId(), msg + " Invoice ID");
        assertEquals(actual.getKbTenantId(), expected.getKbTenantId(), msg + " Tenant ID");
        assertBigDecimalEquals(actual.getTotalTax(), expected.getTotalTax(), 2, msg + " Total tax");
    }

}
