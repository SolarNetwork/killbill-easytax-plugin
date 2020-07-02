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

import static org.killbill.billing.plugin.easytax.dao.gen.tables.EasytaxTaxCodes.EASYTAX_TAX_CODES;
import static org.killbill.billing.plugin.easytax.dao.gen.tables.EasytaxTaxations.EASYTAX_TAXATIONS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.SQLDialect;
import org.jooq.SelectConditionStep;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DSL;
import org.killbill.billing.plugin.dao.PluginDao;
import org.killbill.billing.plugin.easytax.api.EasyTaxDao;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxCode;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;
import org.killbill.billing.plugin.easytax.dao.gen.tables.records.EasytaxTaxCodesRecord;
import org.killbill.billing.plugin.easytax.dao.gen.tables.records.EasytaxTaxationsRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Jooq implementation of {@link EasyTaxDao}.
 * 
 * @author matt
 * @version 2
 */
public class JooqEasyTaxDao extends PluginDao implements EasyTaxDao {

    // CHECKSTYLE OFF: LineLength
    private static final TypeReference<Map<UUID, Set<UUID>>> INVOICE_ITEM_ID_TAX_MAPPING_TYPE = new TypeReference<Map<UUID, Set<UUID>>>() {
    };
    // CHECKSTYLE ON: LineLength

    private final Logger log = LoggerFactory.getLogger(getClass());

    public JooqEasyTaxDao(final DataSource dataSource) throws SQLException {
        super(dataSource);
    }

    public JooqEasyTaxDao(DataSource dataSource, SQLDialect dialect) throws SQLException {
        super(dataSource, dialect);
    }

    @Override
    public void saveTaxCode(final EasyTaxTaxCode taxCode) throws SQLException {
        saveTaxCodes(Collections.singleton(taxCode));
    }

    @Override
    public void saveTaxCodes(final Iterable<EasyTaxTaxCode> taxCodes) throws SQLException {
        final DateTime now = new DateTime();
        execute(dataSource.getConnection(), new WithConnectionCallback<Void>() {
            @Override
            public Void withConnection(final Connection conn) throws SQLException {
                DSL.using(conn, dialect, settings).transaction(new TransactionalRunnable() {
                    @Override
                    public void run(final Configuration configuration) throws Exception {
                        final DSLContext dslContext = DSL.using(configuration);
                        for (EasyTaxTaxCode taxCode : taxCodes) {
                            DateTime date = taxCode.getCreatedDate() != null
                                    ? taxCode.getCreatedDate()
                                    : now;
                            saveTaxCodeInternal(taxCode, date, dslContext);
                        }
                    }
                });
                return null;
            }
        });
    }

    private void saveTaxCodeInternal(final EasyTaxTaxCode taxCode, final DateTime date,
            final DSLContext dslContext) {
        int updateCount = dslContext.update(EASYTAX_TAX_CODES)
                .set(EASYTAX_TAX_CODES.TAX_RATE, taxCode.getTaxRate())
                .set(EASYTAX_TAX_CODES.VALID_FROM_DATE, taxCode.getValidFromDate())
                .set(EASYTAX_TAX_CODES.VALID_TO_DATE, taxCode.getValidToDate())
                .set(EASYTAX_TAX_CODES.CREATED_DATE, date)
                .where(EASYTAX_TAX_CODES.KB_TENANT_ID.equal(taxCode.getKbTenantId().toString()))
                .and(EASYTAX_TAX_CODES.TAX_ZONE.equal(taxCode.getTaxZone()))
                .and(EASYTAX_TAX_CODES.PRODUCT_NAME.equal(taxCode.getProductName()))
                .and(EASYTAX_TAX_CODES.TAX_CODE.equal(taxCode.getTaxCode()))
                .and(EASYTAX_TAX_CODES.VALID_FROM_DATE.equal(taxCode.getValidFromDate())).execute();

        if (updateCount < 1) {
            dslContext
                    .insertInto(EASYTAX_TAX_CODES, EASYTAX_TAX_CODES.KB_TENANT_ID,
                            EASYTAX_TAX_CODES.TAX_ZONE, EASYTAX_TAX_CODES.PRODUCT_NAME,
                            EASYTAX_TAX_CODES.TAX_CODE, EASYTAX_TAX_CODES.TAX_RATE,
                            EASYTAX_TAX_CODES.VALID_FROM_DATE, EASYTAX_TAX_CODES.VALID_TO_DATE,
                            EASYTAX_TAX_CODES.CREATED_DATE)
                    .values(taxCode.getKbTenantId().toString(), taxCode.getTaxZone(),
                            taxCode.getProductName(), taxCode.getTaxCode(), taxCode.getTaxRate(),
                            taxCode.getValidFromDate(), taxCode.getValidToDate(), date)
                    .execute();
        }
    }

    @Override
    public int removeTaxCodes(final UUID kbTenantId, @Nullable final String taxZone,
            @Nullable final String productName, @Nullable final String taxCode)
            throws SQLException {
        return execute(dataSource.getConnection(), new WithConnectionCallback<Integer>() {
            @Override
            public Integer withConnection(final Connection conn) throws SQLException {
                DeleteConditionStep<EasytaxTaxCodesRecord> delete = DSL
                        .using(conn, dialect, settings).delete(EASYTAX_TAX_CODES)
                        .where(EASYTAX_TAX_CODES.KB_TENANT_ID.equal(kbTenantId.toString()));
                if (taxZone != null) {
                    delete = delete.and(EASYTAX_TAX_CODES.TAX_ZONE.equal(taxZone));
                }
                if (productName != null) {
                    delete = delete.and(EASYTAX_TAX_CODES.PRODUCT_NAME.equal(productName));
                }
                if (taxCode != null) {
                    delete = delete.and(EASYTAX_TAX_CODES.TAX_CODE.equal(taxCode));
                }
                return delete.execute();
            }
        });
    }

    @Override
    public List<EasyTaxTaxCode> getTaxCodes(final UUID kbTenantId, @Nullable final String taxZone,
            @Nullable final String productName, @Nullable String taxCode, @Nullable DateTime date)
            throws SQLException {
        List<EasytaxTaxCodesRecord> records = execute(dataSource.getConnection(),
                new WithConnectionCallback<List<EasytaxTaxCodesRecord>>() {
                    @Override
                    public List<EasytaxTaxCodesRecord> withConnection(final Connection conn)
                            throws SQLException {
                        SelectConditionStep<EasytaxTaxCodesRecord> select = DSL
                                .using(conn, dialect, settings).selectFrom(EASYTAX_TAX_CODES)
                                .where(EASYTAX_TAX_CODES.KB_TENANT_ID.equal(kbTenantId.toString()));
                        if (taxZone != null) {
                            select = select.and(EASYTAX_TAX_CODES.TAX_ZONE.equal(taxZone));
                        }
                        if (productName != null) {
                            select = select.and(EASYTAX_TAX_CODES.PRODUCT_NAME.equal(productName));
                        }
                        if (taxCode != null) {
                            select = select.and(EASYTAX_TAX_CODES.TAX_CODE.equal(taxCode));
                        }
                        if (date != null) {
                            select = select.and(EASYTAX_TAX_CODES.VALID_FROM_DATE.lessOrEqual(date))
                                    .and(EASYTAX_TAX_CODES.VALID_TO_DATE.isNull()
                                            .or(EASYTAX_TAX_CODES.VALID_TO_DATE.greaterThan(date)));
                            return select.orderBy(EASYTAX_TAX_CODES.VALID_FROM_DATE.desc()).fetch();
                        } else {
                            return select.orderBy(EASYTAX_TAX_CODES.RECORD_ID.asc()).fetch();
                        }

                    }
                });
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<EasyTaxTaxCode> results = new ArrayList<>();
        for (EasytaxTaxCodesRecord record : records) {
            EasyTaxTaxCode result = new EasyTaxTaxCode();
            result.setCreatedDate(record.getCreatedDate());
            result.setKbTenantId(UUID.fromString(record.getKbTenantId()));
            result.setProductName(record.getProductName());
            result.setTaxZone(record.getTaxZone());
            result.setTaxCode(record.getTaxCode());
            result.setTaxRate(record.getTaxRate());
            result.setValidFromDate(record.getValidFromDate());
            if (record.getValidToDate() != null) {
                result.setValidToDate(record.getValidToDate());
            }
            results.add(result);
        }
        return results;
    }

    @Override
    public void addTaxation(final EasyTaxTaxation taxation) throws SQLException {
        final String invoiceItemIdTaxMappingJson = encodeInvoiceItemIdTaxMapping(
                taxation.getKbInvoiceId(), taxation.getInvoiceItemIds());
        execute(dataSource.getConnection(), new WithConnectionCallback<Void>() {
            @Override
            public Void withConnection(final Connection conn) throws SQLException {
                DSL.using(conn, dialect, settings)
                        .insertInto(EASYTAX_TAXATIONS, EASYTAX_TAXATIONS.KB_TENANT_ID,
                                EASYTAX_TAXATIONS.KB_ACCOUNT_ID, EASYTAX_TAXATIONS.KB_INVOICE_ID,
                                EASYTAX_TAXATIONS.KB_INVOICE_ITEM_IDS, EASYTAX_TAXATIONS.TOTAL_TAX,
                                EASYTAX_TAXATIONS.CREATED_DATE)
                        .values(taxation.getKbTenantId().toString(),
                                taxation.getKbAccountId().toString(),
                                taxation.getKbInvoiceId().toString(), invoiceItemIdTaxMappingJson,
                                taxation.getTotalTax(), taxation.getCreatedDate())
                        .execute();
                return null;
            }
        });
    }

    @Override
    public List<EasyTaxTaxation> getTaxation(final UUID kbTenantId, final UUID kbAccountId,
            final UUID kbInvoiceId) throws SQLException {
        List<EasytaxTaxationsRecord> records = execute(dataSource.getConnection(),
                new WithConnectionCallback<List<EasytaxTaxationsRecord>>() {
                    @Override
                    public List<EasytaxTaxationsRecord> withConnection(final Connection conn)
                            throws SQLException {
                        return DSL.using(conn, dialect, settings).selectFrom(EASYTAX_TAXATIONS)
                                .where(EASYTAX_TAXATIONS.KB_TENANT_ID.equal(kbTenantId.toString()))
                                .and(EASYTAX_TAXATIONS.KB_ACCOUNT_ID.equal(kbAccountId.toString()))
                                .and(EASYTAX_TAXATIONS.KB_INVOICE_ID.equal(kbInvoiceId.toString()))
                                .fetch();
                    }
                });
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(record -> {
            EasyTaxTaxation result = new EasyTaxTaxation();
            result.setRecordId(record.getRecordId().longValue());
            result.setCreatedDate(record.getCreatedDate());
            result.setKbTenantId(UUID.fromString(record.getKbTenantId()));
            result.setKbAccountId(UUID.fromString(record.getKbAccountId()));
            result.setKbInvoiceId(UUID.fromString(record.getKbInvoiceId()));
            result.setInvoiceItemIds(decodeInvoiceItemIdTaxMapping(result.getKbInvoiceId(),
                    record.getKbInvoiceItemIds()));
            result.setTotalTax(record.getTotalTax());
            return result;
        }).collect(Collectors.toList());
    }

    private String encodeInvoiceItemIdTaxMapping(final UUID kbInvoiceId,
            final Map<UUID, Set<UUID>> invoiceItemIdTaxMapping) {
        String invoiceItemIdTaxMappingJson = null;
        try {
            invoiceItemIdTaxMappingJson = (invoiceItemIdTaxMapping != null
                    && !invoiceItemIdTaxMapping.isEmpty()
                            ? objectMapper.writeValueAsString(invoiceItemIdTaxMapping)
                            : null);
        } catch (IOException e) {
            log.warn("Unable to encode invoice item ID tax mapping for invoice_id {}: {}",
                    kbInvoiceId, e.getMessage());
        }
        return invoiceItemIdTaxMappingJson;
    }

    private Map<UUID, Set<UUID>> decodeInvoiceItemIdTaxMapping(final UUID kbInvoiceId,
            String json) {
        Map<UUID, Set<UUID>> invoiceItemIdTaxMapping = null;
        try {
            invoiceItemIdTaxMapping = objectMapper.readValue(json,
                    INVOICE_ITEM_ID_TAX_MAPPING_TYPE);
        } catch (IOException e) {
            log.warn("Unable to dencode invoice item ID tax mapping for invoice_id {}: {}",
                    kbInvoiceId, e.getMessage());
        }
        return invoiceItemIdTaxMapping;
    }

}
