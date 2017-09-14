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

package org.killbill.billing.plugin.easytax.dao;

import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.assertBigDecimalEquals;
import static org.killbill.billing.plugin.easytax.EasyTaxTestUtils.assertDateTimeEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.TestWithEmbeddedDBBase;
import org.killbill.billing.plugin.easytax.EasyTaxTestUtils;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxCode;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the {@link JooqEasyTaxDao} class.
 * 
 * @author matt
 */
public class JooqEasyTaxDaoTests extends TestWithEmbeddedDBBase {

    private static final BigDecimal TEST_TAX_RATE = new BigDecimal("0.15");

    private JooqEasyTaxDao dao;

    private DateTime now;
    private EasyTaxTaxCode lastTaxCode;
    private EasyTaxTaxation lastTaxation;

    @BeforeMethod(groups = "slow")
    public void setUp() throws SQLException, IOException {
        dao = new JooqEasyTaxDao(embeddedDB.getDataSource());
        now = new DateTime().secondOfMinute().roundFloorCopy();
    }

    @Test(groups = "slow")
    public void createTaxCode() throws SQLException {
        final EasyTaxTaxCode taxCode = new EasyTaxTaxCode();
        taxCode.setKbTenantId(UUID.randomUUID());
        taxCode.setTaxZone(UUID.randomUUID().toString());
        taxCode.setProductName(UUID.randomUUID().toString());
        taxCode.setTaxCode(UUID.randomUUID().toString());
        taxCode.setTaxRate(TEST_TAX_RATE);
        taxCode.setCreatedDate(now);
        taxCode.setValidFromDate(now.year().roundFloorCopy());
        taxCode.setValidToDate(now.year().roundCeilingCopy());
        dao.saveTaxCode(taxCode);
        lastTaxCode = taxCode;
    }

    @Test(groups = "slow")
    public void createAndGetTaxCodesForProductTaxCode() throws SQLException {
        createTaxCode();
        EasyTaxTaxCode taxCode = lastTaxCode;
        List<EasyTaxTaxCode> codes = dao.getTaxCodes(taxCode.getKbTenantId(), taxCode.getTaxZone(),
                taxCode.getProductName(), taxCode.getTaxCode(), null);
        assertNotNull(codes, "Results");
        assertEquals(codes.size(), 1, "Result count");

        EasyTaxTaxCode code = codes.get(0);
        assertDateTimeEquals(code.getCreatedDate(), taxCode.getCreatedDate(), "Created");
        assertDateTimeEquals(code.getValidFromDate(), taxCode.getValidFromDate(), "Valid from");
        assertDateTimeEquals(code.getValidToDate(), taxCode.getValidToDate(), "Valid to");
        assertEquals(code.getKbTenantId(), taxCode.getKbTenantId(), "Tenant ID");
        assertEquals(code.getProductName(), taxCode.getProductName(), "Product name");
        assertEquals(code.getTaxCode(), taxCode.getTaxCode(), "Tax code");
        assertBigDecimalEquals(code.getTaxRate(), taxCode.getTaxRate(), 2, "Tax rate");
    }

    @Test(groups = "slow")
    public void createTaxCodes() throws SQLException {
        final EasyTaxTaxCode taxCode = new EasyTaxTaxCode();
        taxCode.setKbTenantId(UUID.randomUUID());
        taxCode.setTaxZone(UUID.randomUUID().toString());
        taxCode.setProductName(UUID.randomUUID().toString());
        taxCode.setTaxCode(UUID.randomUUID().toString());
        taxCode.setTaxRate(TEST_TAX_RATE);
        taxCode.setCreatedDate(now);
        taxCode.setValidFromDate(now.year().roundFloorCopy());
        taxCode.setValidToDate(now.year().roundCeilingCopy());

        final EasyTaxTaxCode taxCode2 = new EasyTaxTaxCode(taxCode);
        taxCode2.setValidFromDate(taxCode.getValidToDate());
        taxCode2.setValidToDate(null);
        taxCode2.setTaxRate(new BigDecimal("0.18"));

        dao.saveTaxCodes(Arrays.asList(taxCode, taxCode2));
        lastTaxCode = taxCode2;
    }

    @Test(groups = "slow")
    public void createMultiAndGetTaxCodesForProductTaxCode() throws SQLException {
        createTaxCodes();
        EasyTaxTaxCode taxCode = lastTaxCode;
        List<EasyTaxTaxCode> codes = dao.getTaxCodes(taxCode.getKbTenantId(), taxCode.getTaxZone(),
                taxCode.getProductName(), taxCode.getTaxCode(), null);
        assertNotNull(codes, "Results");
        assertEquals(codes.size(), 2, "Result count");

        EasyTaxTaxCode code = codes.get(0);
        assertDateTimeEquals(code.getCreatedDate(), now, "Created");
        assertDateTimeEquals(code.getValidFromDate(), now.year().roundFloorCopy(), "Valid from");
        assertDateTimeEquals(code.getValidToDate(), taxCode.getValidFromDate(), "Valid to");
        assertEquals(code.getKbTenantId(), taxCode.getKbTenantId(), "Tenant ID");
        assertEquals(code.getProductName(), taxCode.getProductName(), "Product name");
        assertEquals(code.getTaxCode(), taxCode.getTaxCode(), "Tax code");
        assertBigDecimalEquals(code.getTaxRate(), TEST_TAX_RATE, 2, "Tax rate");

        code = codes.get(1);
        assertDateTimeEquals(code.getCreatedDate(), now, "Created");
        assertDateTimeEquals(code.getValidFromDate(), taxCode.getValidFromDate(), "Valid from");
        assertNull(code.getValidToDate(), "Valid to");
        assertEquals(code.getKbTenantId(), taxCode.getKbTenantId(), "Tenant ID");
        assertEquals(code.getProductName(), taxCode.getProductName(), "Product name");
        assertEquals(code.getTaxCode(), taxCode.getTaxCode(), "Tax code");
        assertBigDecimalEquals(code.getTaxRate(), taxCode.getTaxRate(), 2, "Tax rate");
    }

    @Test(groups = "slow")
    public void getTaxCodesForProductMulti() throws SQLException {
        List<EasyTaxTaxCode> saved = new ArrayList<>();
        createTaxCode();
        saved.add(lastTaxCode);

        EasyTaxTaxCode vat = new EasyTaxTaxCode(lastTaxCode);
        vat.setTaxCode("VAT");
        dao.saveTaxCode(vat);
        saved.add(vat);

        EasyTaxTaxCode mean = new EasyTaxTaxCode(lastTaxCode);
        mean.setTaxCode("MEAN");
        dao.saveTaxCode(mean);
        saved.add(mean);

        List<EasyTaxTaxCode> codes = dao.getTaxCodes(lastTaxCode.getKbTenantId(),
                lastTaxCode.getTaxZone(), lastTaxCode.getProductName(), null, null);

        assertEquals(codes, saved, "All results");
    }

    private List<EasyTaxTaxCode> saveTaxCodeDateRange() throws SQLException {
        List<EasyTaxTaxCode> saved = new ArrayList<>();
        createTaxCode();
        saved.add(lastTaxCode);

        EasyTaxTaxCode c2 = new EasyTaxTaxCode(lastTaxCode);
        c2.setValidFromDate(lastTaxCode.getValidToDate());
        c2.setValidToDate(lastTaxCode.getValidToDate().plusYears(1));
        dao.saveTaxCode(c2);
        saved.add(c2);

        EasyTaxTaxCode c3 = new EasyTaxTaxCode(lastTaxCode);
        c3.setValidFromDate(c2.getValidToDate());
        c3.setValidToDate(null);
        dao.saveTaxCode(c3);
        saved.add(c3);
        return saved;
    }

    @Test(groups = "slow")
    public void saveAndUpdateTaxCodeValidToDate() throws SQLException {
        final DateTime now = new DateTime().secondOfMinute().roundFloorCopy();

        // save initial tax code, with no valid to date
        final EasyTaxTaxCode taxCode = new EasyTaxTaxCode();
        taxCode.setKbTenantId(UUID.randomUUID());
        taxCode.setTaxZone(UUID.randomUUID().toString());
        taxCode.setProductName(UUID.randomUUID().toString());
        taxCode.setTaxCode(UUID.randomUUID().toString());
        taxCode.setTaxRate(TEST_TAX_RATE);
        taxCode.setCreatedDate(now);
        taxCode.setValidFromDate(now.year().roundFloorCopy());
        dao.saveTaxCode(taxCode);

        // now update tax code valid to date
        taxCode.setValidToDate(now);
        dao.saveTaxCode(taxCode);

        List<EasyTaxTaxCode> codes = dao.getTaxCodes(taxCode.getKbTenantId(), taxCode.getTaxZone(),
                taxCode.getProductName(), taxCode.getTaxCode(), null);
        assertNotNull(codes, "Results");
        assertEquals(codes.size(), 1, "Result count");

        EasyTaxTaxCode code = codes.get(0);
        assertDateTimeEquals(code.getCreatedDate(), taxCode.getCreatedDate(), "Created");
        assertDateTimeEquals(code.getValidFromDate(), taxCode.getValidFromDate(), "Valid from");
        assertDateTimeEquals(code.getValidToDate(), taxCode.getValidToDate(), "Valid to");
        assertEquals(code.getKbTenantId(), taxCode.getKbTenantId(), "Tenant ID");
        assertEquals(code.getProductName(), taxCode.getProductName(), "Product name");
        assertEquals(code.getTaxCode(), taxCode.getTaxCode(), "Tax code");
        assertBigDecimalEquals(code.getTaxRate(), taxCode.getTaxRate(), 2, "Tax rate");
    }

    /**
     * Verify can insert multiple tax codes for the same product but different date ranges.
     * 
     * @throws Exception
     *             if an error occurs
     */
    @Test(groups = "slow")
    public void saveTaxCodesForDates() throws Exception {
        List<EasyTaxTaxCode> saved = saveTaxCodeDateRange();

        // check all
        List<EasyTaxTaxCode> found = dao.getTaxCodes(lastTaxCode.getKbTenantId(),
                lastTaxCode.getTaxZone(), lastTaxCode.getProductName(), null, null);
        assertEquals(found, saved, "All matches found");
    }

    @Test(groups = "slow")
    public void getTaxCodeForDate() throws Exception {
        List<EasyTaxTaxCode> saved = saveTaxCodeDateRange();

        // check all
        List<EasyTaxTaxCode> found = dao.getTaxCodes(lastTaxCode.getKbTenantId(),
                lastTaxCode.getTaxZone(), lastTaxCode.getProductName(), null, null);
        assertEquals(found, saved, "All matches found");

        // look before first valid date
        found = dao.getTaxCodes(lastTaxCode.getKbTenantId(), lastTaxCode.getTaxZone(),
                lastTaxCode.getProductName(), null, lastTaxCode.getValidFromDate().minusDays(1));
        assertEquals(found.size(), 0, "No match before earliest valid from date");

        // look within valid date ranges
        for (int i = 0; i < saved.size(); i++) {
            EasyTaxTaxCode code = saved.get(i);
            found = dao.getTaxCodes(lastTaxCode.getKbTenantId(), lastTaxCode.getTaxZone(),
                    lastTaxCode.getProductName(), null, code.getValidFromDate());
            assertEquals(found, Collections.singleton(code),
                    "Matching tax code on valid from date " + i);

            found = dao.getTaxCodes(lastTaxCode.getKbTenantId(), lastTaxCode.getTaxZone(),
                    lastTaxCode.getProductName(), null, code.getValidFromDate().plusMonths(6));
            assertEquals(found, Collections.singleton(code),
                    "Matching tax code in vaild date range " + i);
        }
    }

    @Test(groups = "slow")
    public void saveTaxation() throws SQLException {
        final DateTime now = new DateTime().secondOfMinute().roundFloorCopy();
        EasyTaxTaxation taxation = new EasyTaxTaxation();
        taxation.setCreatedDate(now);
        taxation.setKbTenantId(UUID.randomUUID());
        taxation.setKbAccountId(UUID.randomUUID());
        taxation.setKbInvoiceId(UUID.randomUUID());
        taxation.setTotalTax(new BigDecimal("1.50"));

        final Map<UUID, Set<UUID>> invoiceItemIds = new HashMap<>();
        invoiceItemIds.put(UUID.randomUUID(), Collections.singleton(UUID.randomUUID()));
        taxation.setInvoiceItemIds(invoiceItemIds);

        dao.addTaxation(taxation);
        lastTaxation = taxation;
    }

    @Test(groups = "slow")
    public void saveAndGetTaxation() throws SQLException {
        saveTaxation();

        List<EasyTaxTaxation> taxations = dao.getTaxation(lastTaxation.getKbTenantId(),
                lastTaxation.getKbAccountId(), lastTaxation.getKbInvoiceId());
        assertEquals(taxations.size(), 1, "Saved record count");
        EasyTaxTaxation taxation = taxations.get(0);
        EasyTaxTestUtils.assertEquivalent(taxation, lastTaxation, "");
        lastTaxation = taxation;
    }

    private List<EasyTaxTaxation> saveTaxations(EasyTaxTaxation template, int count)
            throws SQLException {
        List<EasyTaxTaxation> saved = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EasyTaxTaxation tax = new EasyTaxTaxation(template);
            tax.setTotalTax(EasyTaxTestUtils.roundedAmount(new BigDecimal(50 * Math.random()), 2));

            final Map<UUID, Set<UUID>> invoiceItemIds = new HashMap<>();
            invoiceItemIds.put(UUID.randomUUID(), Collections.singleton(UUID.randomUUID()));
            tax.setInvoiceItemIds(invoiceItemIds);

            dao.addTaxation(tax);
            saved.add(tax);
            lastTaxation = tax;
        }
        return saved;
    }

    @Test(groups = "slow")
    public void saveAndGetTaxationsOneInvoiceItem() throws SQLException {
        final DateTime now = new DateTime().secondOfMinute().roundFloorCopy();
        EasyTaxTaxation template = new EasyTaxTaxation();
        template.setCreatedDate(now);
        template.setKbTenantId(UUID.randomUUID());
        template.setKbAccountId(UUID.randomUUID());
        template.setKbInvoiceId(UUID.randomUUID());
        List<EasyTaxTaxation> saved = saveTaxations(template, 3);

        List<EasyTaxTaxation> taxations = dao.getTaxation(template.getKbTenantId(),
                template.getKbAccountId(), template.getKbInvoiceId());
        for (ListIterator<EasyTaxTaxation> itr = taxations.listIterator(); itr.hasNext();) {
            EasyTaxTestUtils.assertEquivalent(itr.next(), saved.get(itr.previousIndex()),
                    String.valueOf(itr.previousIndex()));
        }
    }

    @Test(groups = "slow")
    public void saveAndGetTaxationsMultiInvoiceItems() throws SQLException {
        for (int i = 0; i < 3; i++) {
            final DateTime now = new DateTime().secondOfMinute().roundFloorCopy();
            EasyTaxTaxation template = new EasyTaxTaxation();
            template.setCreatedDate(now);
            template.setKbTenantId(UUID.randomUUID());
            template.setKbAccountId(UUID.randomUUID());
            template.setKbInvoiceId(UUID.randomUUID());
            List<EasyTaxTaxation> saved = saveTaxations(template,
                    (int) Math.ceil(Math.random() * 5));

            List<EasyTaxTaxation> taxations = dao.getTaxation(template.getKbTenantId(),
                    template.getKbAccountId(), template.getKbInvoiceId());
            for (ListIterator<EasyTaxTaxation> itr = taxations.listIterator(); itr.hasNext();) {
                EasyTaxTestUtils.assertEquivalent(itr.next(), saved.get(itr.previousIndex()),
                        String.valueOf(itr.previousIndex()));
            }
        }
    }

}