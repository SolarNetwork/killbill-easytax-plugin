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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Data bean for taxation records.
 * 
 * @author matt
 */
@JsonPropertyOrder({ "created_date", "tenant_id", "account_id", "invoice_id", "total_tax",
        "invoice_item_ids" })
public class EasyTaxTaxation {

    @JsonProperty("record_id")
    private Long recordId;

    @JsonProperty("created_date")
    private DateTime createdDate;

    @JsonProperty("tenant_id")
    private UUID kbTenantId;

    @JsonProperty("account_id")
    private UUID kbAccountId;

    @JsonProperty("invoice_id")
    private UUID kbInvoiceId;

    @JsonProperty("total_tax")
    private BigDecimal totalTax;

    @JsonProperty("invoice_item_ids")
    private Map<UUID, Set<UUID>> invoiceItemIds;

    /**
     * Default constructor.
     */
    public EasyTaxTaxation() {
        super();
    }

    /**
     * Construct with a record ID.
     * 
     * @param recordId
     *            the record ID
     */
    public EasyTaxTaxation(Long recordId) {
        super();
        setRecordId(recordId);
    }

    /**
     * Copy constructor.
     * 
     * <p>
     * Note the {@link #getRecordId()} value is <b>not</b> copied. The {@link #getInvoiceItemIds()}
     * is copied into a new map instance.
     * </p>
     * 
     * @param other
     *            the object to copy
     */
    public EasyTaxTaxation(EasyTaxTaxation other) {
        super();
        setCreatedDate(other.getCreatedDate());
        setKbAccountId(other.getKbAccountId());
        setKbInvoiceId(other.getKbInvoiceId());
        setKbTenantId(other.getKbTenantId());
        setTotalTax(other.getTotalTax());

        if (other.getInvoiceItemIds() != null) {
            setInvoiceItemIds(new HashMap<>(other.getInvoiceItemIds()));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((recordId == null) ? 0 : recordId.hashCode());
        return result;
    }

    /**
     * Test for equality.
     * 
     * <p>
     * Equality for a taxation is defined by the {@link #getRecordId()} field.
     * </p>
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EasyTaxTaxation other = (EasyTaxTaxation) obj;
        if (recordId == null) {
            if (other.recordId != null) {
                return false;
            }
        } else if (!recordId.equals(other.recordId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EasyTaxTaxation{recordId=" + recordId + ", createdDate=" + createdDate
                + ", kbInvoiceId=" + kbInvoiceId + ", totalTax=" + totalTax + "}";
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(DateTime createdDate) {
        this.createdDate = createdDate;
    }

    public UUID getKbTenantId() {
        return kbTenantId;
    }

    public void setKbTenantId(UUID kbTenantId) {
        this.kbTenantId = kbTenantId;
    }

    public UUID getKbAccountId() {
        return kbAccountId;
    }

    public void setKbAccountId(UUID kbAccountId) {
        this.kbAccountId = kbAccountId;
    }

    public UUID getKbInvoiceId() {
        return kbInvoiceId;
    }

    public void setKbInvoiceId(UUID kbInvoiceId) {
        this.kbInvoiceId = kbInvoiceId;
    }

    public Map<UUID, Set<UUID>> getInvoiceItemIds() {
        return invoiceItemIds;
    }

    public void setInvoiceItemIds(Map<UUID, Set<UUID>> invoiceItemIds) {
        this.invoiceItemIds = invoiceItemIds;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

}
