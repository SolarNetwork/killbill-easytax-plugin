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
import java.util.UUID;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

/**
 * Data bean for tax code records.
 * 
 * @author matt
 */
@JsonPropertyOrder({ "created_date", "tenant_id", "tax_zone", "product_name", "tax_code",
        "tax_rate", "valid_from_date", "valid_to_date" })
public class EasyTaxTaxCode {

    /** A supporting JSON view for full details. */
    public static final class FullView {
        // nothing
    }

    @JsonProperty("created_date")
    private DateTime createdDate;

    @JsonView(FullView.class)
    @JsonProperty("tenant_id")
    private UUID kbTenantId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("tax_zone")
    private String taxZone;

    @JsonProperty("tax_code")
    private String taxCode;

    @JsonProperty("tax_rate")
    private BigDecimal taxRate;

    @JsonProperty("valid_from_date")
    private DateTime validFromDate;

    @JsonProperty("valid_to_date")
    private DateTime validToDate;

    /**
     * Default constructor.
     */
    public EasyTaxTaxCode() {
        super();
    }

    /**
     * Construct with a tax code.
     * 
     * @param taxCode
     *            the tax code
     */
    public EasyTaxTaxCode(String taxCode) {
        super();
        setTaxCode(taxCode);
    }

    /**
     * Copy constructor.
     * 
     * @param other
     *            the tax code data to copy
     */
    public EasyTaxTaxCode(EasyTaxTaxCode other) {
        super();
        setCreatedDate(other.getCreatedDate());
        setKbTenantId(other.getKbTenantId());
        setProductName(other.getProductName());
        setTaxZone(other.getTaxZone());
        setTaxCode(other.getTaxCode());
        setTaxRate(other.getTaxRate());
        setValidFromDate(other.getValidFromDate());
        setValidToDate(other.getValidToDate());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((kbTenantId == null) ? 0 : kbTenantId.hashCode());
        result = prime * result + ((productName == null) ? 0 : productName.hashCode());
        result = prime * result + ((taxCode == null) ? 0 : taxCode.hashCode());
        result = prime * result + ((taxZone == null) ? 0 : taxZone.hashCode());
        result = prime * result + ((validFromDate == null) ? 0 : validFromDate.hashCode());
        return result;
    }

    /**
     * Test for equality.
     * 
     * <p>
     * Equality for a tax code is defined by the following fields:
     * </p>
     * 
     * <ol>
     * <li>{@link #getKbTenantId()}</li>
     * <li>{@link #getTaxZone()}</li>
     * <li>{@link #getProductName()}</li>
     * <li>{@link #getTaxCode()}</li>
     * <li>{@link #getValidFromDate()}</li>
     * </ol>
     * 
     * <p>
     * Note the {@code validToDate} is <b>not</b> used.
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
        EasyTaxTaxCode other = (EasyTaxTaxCode) obj;
        if (kbTenantId == null) {
            if (other.kbTenantId != null) {
                return false;
            }
        } else if (!kbTenantId.equals(other.kbTenantId)) {
            return false;
        }
        if (productName == null) {
            if (other.productName != null) {
                return false;
            }
        } else if (!productName.equals(other.productName)) {
            return false;
        }
        if (taxCode == null) {
            if (other.taxCode != null) {
                return false;
            }
        } else if (!taxCode.equals(other.taxCode)) {
            return false;
        }
        if (taxZone == null) {
            if (other.taxZone != null) {
                return false;
            }
        } else if (!taxZone.equals(other.taxZone)) {
            return false;
        }
        if (validFromDate == null) {
            if (other.validFromDate != null) {
                return false;
            }
        } else if (!validFromDate.isEqual(other.validFromDate)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EasyTaxTaxCode{taxZone=" + taxZone + ", productName=" + productName + ", taxCode="
                + taxCode + ", taxRate=" + taxRate + "}";
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

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getTaxZone() {
        return taxZone;
    }

    public void setTaxZone(String taxZone) {
        this.taxZone = taxZone;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public DateTime getValidFromDate() {
        return validFromDate;
    }

    public void setValidFromDate(DateTime validFromDate) {
        this.validFromDate = validFromDate;
    }

    public DateTime getValidToDate() {
        return validToDate;
    }

    public void setValidToDate(DateTime validToDate) {
        this.validToDate = validToDate;
    }

}
