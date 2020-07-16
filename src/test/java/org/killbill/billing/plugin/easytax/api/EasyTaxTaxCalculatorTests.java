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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.assertBigDecimalEquals;
import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.createOptionalService;
import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.prettyPrintInvoiceAndComputedTaxItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.catalog.api.Product;
import org.killbill.billing.catalog.api.StaticCatalog;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.plugin.TestUtils;
import org.killbill.billing.plugin.api.invoice.PluginTaxCalculator;
import org.killbill.billing.plugin.easytax.CatalogUtils;
import org.killbill.billing.plugin.easytax.EasyTaxTestUtils;
import org.killbill.billing.plugin.easytax.core.EasyTaxActivator;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfig;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfigurationHandler;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxCode;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.clock.Clock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test cases for the {@link EasyTaxTaxCalculator} class.
 * 
 * @author matt
 */
public class EasyTaxTaxCalculatorTests {

    private static final String GST = "GST";
    private static final String XST = "XST";

    private static final String TEST_PRODUCT_NAME = "test-product";
    private static final String TEST_PLAN_NAME = "test-plan";

    private static final BigDecimal GST_RATE = new BigDecimal("0.15");
    private static final BigDecimal XST_RATE = new BigDecimal("0.385");

    private static final AtomicLong RECORD_ID = new AtomicLong((long) (Math.random() * 1000));

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Clock clock;
    private DateTime now;
    private UUID tenantId;
    private Account account1;
    private Account account2;
    private Invoice newInvoice1;
    private Invoice newInvoice2;
    private EasyTaxDao dao;

    private EasyTaxTaxCode nzGst;
    private EasyTaxTaxCode xst;
    private Product testProduct;
    private StaticCatalog currCatalog;

    private OSGIKillbillAPI osgiKillbillApi;
    @SuppressWarnings("deprecation")
    private org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService osgiKillbillLogService;
    private EasyTaxConfig config = new EasyTaxConfig(new Properties());

    @BeforeMethod
    public void setup() throws Exception {
        clock = Mockito.mock(Clock.class);
        now = new DateTime(DateTimeZone.UTC);
        tenantId = UUID.randomUUID();

        account1 = TestUtils.buildAccount(Currency.NZD, "NZ");
        account2 = TestUtils.buildAccount(Currency.EUR, "DE");

        newInvoice1 = TestUtils.buildInvoice(account1);
        newInvoice2 = TestUtils.buildInvoice(account2);

        dao = Mockito.mock(EasyTaxDao.class);

        osgiKillbillApi = TestUtils.buildOSGIKillbillAPI(account1);

        osgiKillbillLogService = TestUtils.buildLogService();

        when(clock.getUTCNow()).thenReturn(now);

        // no custom fields provided for tests
        CustomFieldUserApi fieldApi = Mockito.mock(CustomFieldUserApi.class);
        when(osgiKillbillApi.getCustomFieldUserApi()).thenReturn(fieldApi);
        when(fieldApi.getCustomFieldsForObject(any(), eq(ObjectType.ACCOUNT), any()))
                .thenReturn(Collections.emptyList());

        EasyTaxTaxCode taxCode = new EasyTaxTaxCode();
        taxCode.setKbTenantId(tenantId);
        taxCode.setTaxZone("NZ");
        taxCode.setProductName(TEST_PRODUCT_NAME);
        taxCode.setTaxCode(GST);
        taxCode.setTaxRate(GST_RATE);
        taxCode.setValidFromDate(new DateTime().year().roundFloorCopy());
        nzGst = taxCode;

        taxCode = new EasyTaxTaxCode();
        taxCode.setKbTenantId(tenantId);
        taxCode.setTaxZone("NZ");
        taxCode.setProductName(TEST_PRODUCT_NAME);
        taxCode.setTaxCode(XST);
        taxCode.setTaxRate(XST_RATE);
        taxCode.setValidFromDate(new DateTime().year().roundFloorCopy());
        xst = taxCode;

        // add catalog support with test product and test plan
        currCatalog = Mockito.mock(StaticCatalog.class);
        testProduct = Mockito.mock(Product.class);
        given(testProduct.getName()).willReturn(TEST_PRODUCT_NAME);
        Plan plan = Mockito.mock(Plan.class);
        given(plan.getProduct()).willReturn(testProduct);
        given(currCatalog.findPlan(TEST_PLAN_NAME)).willReturn(plan);
        CatalogUtils.setupCatalogApi(osgiKillbillApi, CatalogUtils.createCatalogApi(currCatalog));
    }

    @AfterMethod
    public void teardown() {
        Mockito.validateMockitoUsage();
    }

    private InvoiceItem invoiceItemForTestPlan(Invoice invoice, BigDecimal amount) {
        return invoiceItemForTestPlan(invoice, InvoiceItemType.USAGE, amount, null);
    }

    private InvoiceItem invoiceItemForTestPlan(Invoice invoice, InvoiceItemType type,
            BigDecimal amount, UUID linkedItemId) {
        InvoiceItem item = TestUtils.buildInvoiceItem(invoice, type, amount, linkedItemId);
        when(item.getDescription()).thenReturn(UUID.randomUUID().toString());
        when(item.getPlanName()).thenReturn(TEST_PLAN_NAME);
        return item;
    }

    private EasyTaxTaxCalculator calculatorWithConfig(EasyTaxConfig config) {
        final EasyTaxConfigurationHandler easyTaxConfigurationHandler = new EasyTaxConfigurationHandler(
                EasyTaxActivator.PLUGIN_NAME, osgiKillbillApi, osgiKillbillLogService);
        easyTaxConfigurationHandler.setDefaultConfigurable(config);
        return new EasyTaxTaxCalculator(osgiKillbillApi, easyTaxConfigurationHandler, dao,
                createOptionalService((EasyTaxTaxZoneResolver) null, null),
                createOptionalService((EasyTaxTaxDateResolver) null, null), clock);
    }

    @Test(groups = "fast")
    public void invoiceItemOnNewInvoice() throws Exception {
        // given
        final PluginTaxCalculator calculator = calculatorWithConfig(config);

        final Invoice invoice = TestUtils.buildInvoice(account1);
        final DateTime invoiceTaxDate = invoice.getInvoiceDate()
                .toDateTimeAtStartOfDay(account1.getTimeZone());
        final InvoiceItem taxableItem1 = invoiceItemForTestPlan(invoice, new BigDecimal("100"));
        final Map<UUID, InvoiceItem> taxableItems1 = singletonMap(taxableItem1.getId(),
                taxableItem1);
        final Map<UUID, Collection<InvoiceItem>> adjustmentItems1 = Collections.emptyMap();

        // query for applicable tax codes will return GST for account
        given(dao.getTaxCodes(tenantId, account1.getCountry(), TEST_PRODUCT_NAME, null,
                invoiceTaxDate)).willReturn(singletonList(nzGst));

        // no taxation records exist yet
        given(dao.getTaxation(tenantId, account1.getId(), invoice.getId())).willReturn(emptyList());

        // when
        final List<InvoiceItem> initialTaxItems = calculator.compute(account1, newInvoice1, invoice,
                taxableItems1, adjustmentItems1, false, emptyList(), tenantId);

        log.info(prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(), adjustmentItems1,
                initialTaxItems));

        // then
        assertEquals(initialTaxItems.size(), 1);
        InvoiceItem taxItem1 = initialTaxItems.get(0);
        assertEquals(taxItem1.getAccountId(), account1.getId());
        assertEquals(taxItem1.getInvoiceId(), newInvoice1.getId());
        assertEquals(taxItem1.getInvoiceItemType(), InvoiceItemType.TAX);
        assertBigDecimalEquals(taxItem1.getAmount(),
                taxableItem1.getAmount().multiply(nzGst.getTaxRate()), 2, "Tax amount");
        assertEquals(taxItem1.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");

        // verify what was stored in the taxation table
        ArgumentCaptor<EasyTaxTaxation> taxationCaptor = ArgumentCaptor
                .forClass(EasyTaxTaxation.class);
        then(dao).should().addTaxation(taxationCaptor.capture());

        EasyTaxTaxation taxation = taxationCaptor.getValue();
        assertEquals(taxation.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation.getKbAccountId(), account1.getId(), "Taxation account1");
        assertEquals(taxation.getKbInvoiceId(), invoice.getId(), "Taxation invoice");
        assertEquals(taxation.getInvoiceItemIds(), Collections.singletonMap(taxableItem1.getId(),
                Collections.singleton(taxItem1.getId())));
        assertBigDecimalEquals(taxation.getTotalTax(), taxItem1.getAmount(), 2,
                "Taxation total tax");
    }

    @Test(groups = "fast")
    public void invoiceItemOnNewInvoiceMultipleTaxes() throws Exception {
        // given
        final PluginTaxCalculator calculator = calculatorWithConfig(config);

        final Invoice invoice = TestUtils.buildInvoice(account1);
        final DateTime invoiceTaxDate = invoice.getInvoiceDate()
                .toDateTimeAtStartOfDay(account1.getTimeZone());
        final InvoiceItem taxableItem1 = invoiceItemForTestPlan(invoice, new BigDecimal("100"));
        final Map<UUID, InvoiceItem> taxableItems1 = singletonMap(taxableItem1.getId(),
                taxableItem1);
        final Map<UUID, Collection<InvoiceItem>> adjustmentItems1 = Collections.emptyMap();

        // query for applicable tax codes will return GST for account
        given(dao.getTaxCodes(tenantId, account1.getCountry(), TEST_PRODUCT_NAME, null,
                invoiceTaxDate)).willReturn(asList(nzGst, xst));

        // no taxation records exist yet
        given(dao.getTaxation(tenantId, account1.getId(), invoice.getId())).willReturn(emptyList());

        // when
        final List<InvoiceItem> initialTaxItems = calculator.compute(account1, newInvoice1, invoice,
                taxableItems1, adjustmentItems1, false, emptyList(), tenantId);

        log.info(prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(), adjustmentItems1,
                initialTaxItems));

        // then
        assertEquals(initialTaxItems.size(), 2);
        InvoiceItem taxItem1 = initialTaxItems.get(0);
        assertEquals(taxItem1.getAccountId(), account1.getId());
        assertEquals(taxItem1.getInvoiceId(), newInvoice1.getId());
        assertEquals(taxItem1.getInvoiceItemType(), InvoiceItemType.TAX);
        assertBigDecimalEquals(taxItem1.getAmount(),
                taxableItem1.getAmount().multiply(nzGst.getTaxRate()), 2, "Tax amount");
        assertEquals(taxItem1.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");

        InvoiceItem taxItem2 = initialTaxItems.get(1);
        assertEquals(taxItem2.getAccountId(), account1.getId());
        assertEquals(taxItem2.getInvoiceId(), newInvoice1.getId());
        assertEquals(taxItem2.getInvoiceItemType(), InvoiceItemType.TAX);
        assertBigDecimalEquals(taxItem2.getAmount(),
                taxableItem1.getAmount().multiply(xst.getTaxRate()), 2, "Tax amount");
        assertEquals(taxItem2.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");

        // verify what was stored in the taxation table
        ArgumentCaptor<EasyTaxTaxation> taxationCaptor = ArgumentCaptor
                .forClass(EasyTaxTaxation.class);
        then(dao).should().addTaxation(taxationCaptor.capture());

        EasyTaxTaxation taxation = taxationCaptor.getValue();
        assertEquals(taxation.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation.getKbAccountId(), account1.getId(), "Taxation account1");
        assertEquals(taxation.getKbInvoiceId(), invoice.getId(), "Taxation invoice");
        assertEquals(taxation.getInvoiceItemIds(), Collections.singletonMap(taxableItem1.getId(),
                new HashSet<>(asList(taxItem1.getId(), taxItem2.getId()))));
        assertBigDecimalEquals(taxation.getTotalTax(),
                taxItem1.getAmount().add(taxItem2.getAmount()), 2, "Taxation total tax");
    }

    @Test(groups = "fast")
    public void invoiceItemAdjustmentOnNewInvoice() throws Exception {
        // given
        final PluginTaxCalculator calculator = calculatorWithConfig(config);

        final Invoice invoice = TestUtils.buildInvoice(account1);
        final DateTime invoiceTaxDate = invoice.getInvoiceDate()
                .toDateTimeAtStartOfDay(account1.getTimeZone());
        final InvoiceItem taxableItem1 = invoiceItemForTestPlan(invoice, new BigDecimal("100"));
        final Map<UUID, InvoiceItem> taxableItems1 = singletonMap(taxableItem1.getId(),
                taxableItem1);

        final InvoiceItem adjustment1ForInvoiceItem1 = invoiceItemForTestPlan(invoice,
                InvoiceItemType.ITEM_ADJ, BigDecimal.ONE.negate(), taxableItem1.getId());
        final Map<UUID, Collection<InvoiceItem>> adjustmentItems1 = singletonMap(
                taxableItem1.getId(), singleton(adjustment1ForInvoiceItem1));

        // query for applicable tax codes will return GST for account
        given(dao.getTaxCodes(tenantId, account1.getCountry(), TEST_PRODUCT_NAME, null,
                invoiceTaxDate)).willReturn(singletonList(nzGst));

        // no taxation records exist yet
        given(dao.getTaxation(tenantId, account1.getId(), invoice.getId())).willReturn(emptyList());

        // when
        final List<InvoiceItem> initialTaxItems = calculator.compute(account1, invoice, invoice,
                taxableItems1, adjustmentItems1, false, emptyList(), tenantId);

        log.info(prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(), adjustmentItems1,
                initialTaxItems));

        // then
        assertEquals(initialTaxItems.size(), 2);
        InvoiceItem taxItem1 = initialTaxItems.get(0);
        assertEquals(taxItem1.getAccountId(), account1.getId());
        assertEquals(taxItem1.getInvoiceId(), invoice.getId());
        assertEquals(taxItem1.getInvoiceItemType(), InvoiceItemType.TAX);
        assertEquals(taxItem1.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");
        assertBigDecimalEquals(taxItem1.getAmount(), taxableItem1.getAmount().multiply(GST_RATE), 2,
                "Tax amount");

        InvoiceItem taxItem2 = initialTaxItems.get(1);
        assertEquals(taxItem2.getAccountId(), account1.getId());
        assertEquals(taxItem2.getInvoiceId(), invoice.getId());
        assertEquals(taxItem2.getInvoiceItemType(), InvoiceItemType.TAX);
        assertEquals(taxItem2.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");
        assertBigDecimalEquals(taxItem2.getAmount(),
                adjustment1ForInvoiceItem1.getAmount().multiply(GST_RATE), 2, "Tax amount");

        // verify what was stored in the taxation table
        ArgumentCaptor<EasyTaxTaxation> taxationCaptor = ArgumentCaptor
                .forClass(EasyTaxTaxation.class);
        then(dao).should().addTaxation(taxationCaptor.capture());

        EasyTaxTaxation taxation = taxationCaptor.getValue();
        assertEquals(taxation.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation.getKbAccountId(), account1.getId(), "Taxation account1");
        assertEquals(taxation.getKbInvoiceId(), invoice.getId(), "Taxation invoice");
        assertEquals(taxation.getInvoiceItemIds(),
                Collections.singletonMap(taxableItem1.getId(),
                        new HashSet<>(asList(taxItem1.getId(), taxItem2.getId(),
                                adjustment1ForInvoiceItem1.getId()))));
        assertBigDecimalEquals(taxation.getTotalTax(), new BigDecimal("14.85"), 2,
                "Total tax adjusted $100-$1 @ 15%");
    }

    @Test(enabled = true, description = "TODO: this test needs work")
    public void invoiceItemsOverTime() throws Exception {
        // given
        final PluginTaxCalculator calculator = calculatorWithConfig(config);

        testComputeItemsOverTime(calculator, currCatalog);
    }

    private void testComputeItemsOverTime(final PluginTaxCalculator calculator,
            StaticCatalog currCatalog) throws Exception {
        testComputeItemsOverTime(calculator, account1, newInvoice1, currCatalog);
        testComputeItemsOverTime(calculator, account2, newInvoice2, currCatalog);
    }

    private void testComputeItemsOverTime(final PluginTaxCalculator calculator,
            final Account account, final Invoice newInvoice, StaticCatalog currCatalog)
            throws Exception {
        // given
        final Invoice invoice = TestUtils.buildInvoice(account);
        final DateTime invoiceTaxDate = invoice.getInvoiceDate()
                .toDateTimeAtStartOfDay(account.getTimeZone());

        final InvoiceItem taxableItem1 = invoiceItemForTestPlan(invoice,
                InvoiceItemType.EXTERNAL_CHARGE, new BigDecimal("100"), null);
        final InvoiceItem taxableItem2 = invoiceItemForTestPlan(invoice, InvoiceItemType.RECURRING,
                BigDecimal.TEN, null);

        final Map<UUID, InvoiceItem> taxableItems1 = ImmutableMap.<UUID, InvoiceItem>of(
                taxableItem1.getId(), taxableItem1, taxableItem2.getId(), taxableItem2);
        final Map<UUID, Collection<InvoiceItem>> initialAdjustmentItems = emptyMap();

        // query for applicable tax codes will return GST for account
        given(dao.getTaxCodes(tenantId, account.getCountry(), TEST_PRODUCT_NAME, null,
                invoiceTaxDate)).willReturn(singletonList(nzGst));

        // no taxation records exist yet
        given(dao.getTaxation(tenantId, account.getId(), invoice.getId())).willReturn(emptyList());

        // verify what was stored in the taxation table
        ArgumentCaptor<EasyTaxTaxation> taxationCaptor = ArgumentCaptor
                .forClass(EasyTaxTaxation.class);
        Mockito.doNothing().when(dao).addTaxation(taxationCaptor.capture());

        // when

        final List<InvoiceItem> initialTaxItems = calculator.compute(account, newInvoice, invoice,
                taxableItems1, initialAdjustmentItems, false, emptyList(), tenantId);
        log.info(EasyTaxTestUtils.prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(),
                initialAdjustmentItems, initialTaxItems));

        // then
        checkCreatedItems(ImmutableMap.<UUID, InvoiceItemType>of(taxableItem1.getId(),
                InvoiceItemType.TAX, taxableItem2.getId(), InvoiceItemType.TAX), initialTaxItems,
                newInvoice);
        assertEquals(initialTaxItems.size(), 2, "Initial tax item count for $100, $10");
        InvoiceItem taxItem1 = initialTaxItems.get(0);
        assertEquals(taxItem1.getAccountId(), account.getId());
        assertEquals(taxItem1.getInvoiceId(), newInvoice.getId());
        assertEquals(taxItem1.getInvoiceItemType(), InvoiceItemType.TAX);
        assertEquals(taxItem1.getLinkedItemId(), taxableItem1.getId(), "Linked to taxable item");
        assertBigDecimalEquals(taxItem1.getAmount(), taxableItem1.getAmount().multiply(GST_RATE), 2,
                "Tax amount");

        InvoiceItem taxItem2 = initialTaxItems.get(1);
        assertEquals(taxItem2.getAccountId(), account.getId());
        assertEquals(taxItem2.getInvoiceId(), newInvoice.getId());
        assertEquals(taxItem2.getInvoiceItemType(), InvoiceItemType.TAX);
        assertEquals(taxItem2.getLinkedItemId(), taxableItem2.getId(), "Linked to taxable item");
        assertBigDecimalEquals(taxItem2.getAmount(), taxableItem2.getAmount().multiply(GST_RATE), 2,
                "Tax amount");

        EasyTaxTaxation taxation = taxationCaptor.getValue();
        assertEquals(taxation.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation.getKbAccountId(), account.getId(), "Taxation account");
        assertEquals(taxation.getKbInvoiceId(), invoice.getId(), "Taxation invoice");
        assertEquals(taxation.getInvoiceItemIds(),
                ImmutableMap.<UUID, Set<UUID>>of(taxableItem1.getId(),
                        singleton(initialTaxItems.get(0).getId()), taxableItem2.getId(),
                        singleton(initialTaxItems.get(1).getId())),
                "Invoice item IDs for all taxable items");
        assertBigDecimalEquals(taxation.getTotalTax(),
                taxableItem1.getAmount().add(taxableItem2.getAmount()).multiply(GST_RATE), 2,
                "Total tax adjusted $100+$10 @ 15%");

        // given 1 taxation record exists now
        taxation.setRecordId(RECORD_ID.incrementAndGet()); // assign unique ID like DB would
        given(dao.getTaxation(tenantId, account.getId(), invoice.getId()))
                .willReturn(singletonList(taxation));

        // verify compute is idempotent
        assertEquals(calculator.compute(account, newInvoice, invoice, taxableItems1,
                initialAdjustmentItems, false, emptyList(), tenantId).size(), 0);
        assertEquals(taxationCaptor.getAllValues().size(), 1, "Taxation records unchanged");

        // add an adjustment (subtract $1 from $100 charge)
        final InvoiceItem adjustment1ForInvoiceItem1 = TestUtils.buildInvoiceItem(invoice,
                InvoiceItemType.ITEM_ADJ, BigDecimal.ONE.negate(), taxableItem1.getId());
        final Map<UUID, Collection<InvoiceItem>> subsequentAdjustmentItems1 = singletonMap(
                taxableItem1.getId(), singleton(adjustment1ForInvoiceItem1));

        // when
        final List<InvoiceItem> adjustments1 = calculator.compute(account, newInvoice, invoice,
                taxableItems1, subsequentAdjustmentItems1, false, emptyList(), tenantId);
        log.info(prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(),
                subsequentAdjustmentItems1, adjustments1));

        // then
        checkCreatedItems(
                ImmutableMap.<UUID, InvoiceItemType>of(taxableItem1.getId(), InvoiceItemType.TAX),
                adjustments1, newInvoice);
        assertEquals(adjustments1.size(), 1, "Adjustment tax item count for $100-$1");

        assertEquals(adjustments1.get(0).getAccountId(), account.getId());
        assertEquals(adjustments1.get(0).getInvoiceId(), newInvoice.getId());
        assertEquals(adjustments1.get(0).getInvoiceItemType(), InvoiceItemType.TAX);
        assertEquals(adjustments1.get(0).getLinkedItemId(), taxableItem1.getId(),
                "Linked to taxable item");
        assertBigDecimalEquals(adjustments1.get(0).getAmount(),
                adjustment1ForInvoiceItem1.getAmount().multiply(GST_RATE), 2,
                "Tax amount for -$1 adjustment");

        assertEquals(taxationCaptor.getAllValues().size(), 2, "Taxation record added");
        EasyTaxTaxation taxation2 = taxationCaptor.getValue();
        assertEquals(taxation2.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation2.getKbAccountId(), account.getId(), "Taxation account");
        assertEquals(taxation2.getKbInvoiceId(), invoice.getId(), "Taxation invoice");
        assertEquals(taxation2.getInvoiceItemIds(), ImmutableMap.of(taxableItem1.getId(),
                new HashSet<>(
                        asList(adjustments1.get(0).getId(), adjustment1ForInvoiceItem1.getId()))),
                "Taxaction records new tax and adjustment references");
        assertBigDecimalEquals(taxation2.getTotalTax(),
                adjustment1ForInvoiceItem1.getAmount().multiply(GST_RATE), 2,
                "Total tax for $1 adjustment");

        // given 2 taxation records exist now
        taxation2.setRecordId(RECORD_ID.incrementAndGet()); // assign unique ID like DB would
        given(dao.getTaxation(tenantId, account.getId(), invoice.getId()))
                .willReturn(asList(taxation, taxation2));

        // verify compute is idempotent
        final List<InvoiceItem> adjustments1idemp = calculator.compute(account, newInvoice, invoice,
                taxableItems1, subsequentAdjustmentItems1, false, emptyList(), tenantId);
        log.info(prettyPrintInvoiceAndComputedTaxItems(taxableItems1.values(),
                subsequentAdjustmentItems1, adjustments1idemp));
        assertEquals(adjustments1idemp.size(), 0, "Re-adjustment idempotent");
        assertEquals(taxationCaptor.getAllValues().size(), 2, "Taxation records unchanged");

        // Compute a subsequent adjustment (with a new item on a new invoice this time, to simulate
        // a repair)
        final InvoiceItem adjustment2ForInvoiceItem1 = TestUtils.buildInvoiceItem(invoice,
                InvoiceItemType.ITEM_ADJ, BigDecimal.TEN.negate(), taxableItem1.getId());
        final Invoice adjustmentInvoice = TestUtils.buildInvoice(account);
        final InvoiceItem adjustment1ForInvoiceItem2 = TestUtils.buildInvoiceItem(adjustmentInvoice,
                InvoiceItemType.REPAIR_ADJ, BigDecimal.ONE.negate(), taxableItem2.getId());
        final InvoiceItem taxableItem3 = TestUtils.buildInvoiceItem(adjustmentInvoice,
                InvoiceItemType.RECURRING, BigDecimal.TEN, null);
        final Map<UUID, InvoiceItem> taxableItems2 = ImmutableMap.<UUID, InvoiceItem>of(
                taxableItem1.getId(), taxableItem1, taxableItem2.getId(), taxableItem2,
                taxableItem3.getId(), taxableItem3);
        final ImmutableMap<UUID, Collection<InvoiceItem>> subsequentAdjustmentItems2 = ImmutableMap
                .<UUID, Collection<InvoiceItem>>of(taxableItem1.getId(),
                        ImmutableList.<InvoiceItem>of(adjustment2ForInvoiceItem1),
                        taxableItem2.getId(),
                        ImmutableList.<InvoiceItem>of(adjustment1ForInvoiceItem2));

        // when
        final List<InvoiceItem> adjustments2 = calculator.compute(account, newInvoice,
                adjustmentInvoice, taxableItems2, subsequentAdjustmentItems2, false, emptyList(),
                tenantId);
        log.info(EasyTaxTestUtils.prettyPrintInvoiceAndComputedTaxItems(taxableItems2.values(),
                subsequentAdjustmentItems2, adjustments2));

        // then

        // Check the created items
        checkCreatedItems(ImmutableMap.<UUID, InvoiceItemType>of(taxableItem1.getId(),
                InvoiceItemType.TAX, taxableItem2.getId(), InvoiceItemType.TAX,
                taxableItem3.getId(), InvoiceItemType.TAX), adjustments2, newInvoice);
        assertEquals(adjustments2.size(), 4, "Adjustment tax item count for $100-$1");

        assertEquals(taxationCaptor.getAllValues().size(), 3, "Taxation record added");
        EasyTaxTaxation taxation3 = taxationCaptor.getValue();
        assertEquals(taxation3.getKbTenantId(), tenantId, "Taxation tenant");
        assertEquals(taxation3.getKbAccountId(), account.getId(), "Taxation account");
        assertEquals(taxation3.getKbInvoiceId(), adjustmentInvoice.getId(), "Taxation invoice");
        assertBigDecimalEquals(taxation3.getTotalTax(), new BigDecimal("14.85"), 2, "Total tax");

        // given 3 taxation records exist now (2 for invoice, 1 for adjustmentInvoice)
        taxation3.setRecordId(RECORD_ID.incrementAndGet()); // assign unique ID like DB would
        given(dao.getTaxation(tenantId, account.getId(), adjustmentInvoice.getId()))
                .willReturn(asList(taxation3));

        // verify compute is idempotent
        final List<InvoiceItem> adjustments2idemp = calculator.compute(account, newInvoice,
                adjustmentInvoice, taxableItems2, subsequentAdjustmentItems2, false, emptyList(),
                tenantId);
        assertEquals(adjustments2idemp.size(), 0);
    }

    private void checkCreatedItems(final Map<UUID, InvoiceItemType> expectedInvoiceItemTypes,
            final Iterable<InvoiceItem> createdItems, final Invoice newInvoice) {
        for (final InvoiceItem invoiceItem : createdItems) {
            Assert.assertEquals(invoiceItem.getInvoiceId(), newInvoice.getId());
            Assert.assertEquals(invoiceItem.getInvoiceItemType(),
                    expectedInvoiceItemTypes.get(invoiceItem.getLinkedItemId()));
        }
    }
}
