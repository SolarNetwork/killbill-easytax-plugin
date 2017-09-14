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

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxCode;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;

/**
 * API for EasyTax data access.
 * 
 * @author matt
 */
public interface EasyTaxDao {

    /**
     * Add or update a tax code.
     * 
     * <p>
     * This method will treat tax codes as being uniquely defined as outlined in
     * {@link EasyTaxTaxCode#equals(Object)}. Thus multiple tax codes <b>cannot</b> be defined for
     * the same <i>tax zone</i>, <i>product name</i>, <i>tax code</i>, and <i>valid from</i> date
     * values.
     * </p>
     * 
     * @param taxCode
     *            the tax code details to save
     * @throws SQLException
     *             if any SQL error occurs
     */
    void saveTaxCode(EasyTaxTaxCode taxCode) throws SQLException;

    /**
     * Add or update multiple tax codes.
     * 
     * <p>
     * This method will treat tax codes as being uniquely defined as outlined in
     * {@link EasyTaxTaxCode#equals(Object)}. Thus multiple tax codes <b>cannot</b> be defined for
     * the same <i>tax zone</i>, <i>product name</i>, <i>tax code</i>, and <i>valid from</i> date
     * values.
     * </p>
     * 
     * @param taxCodes
     *            the tax code details to save
     * @throws SQLException
     *             if any SQL error occurs
     */
    void saveTaxCodes(Iterable<EasyTaxTaxCode> taxCodes) throws SQLException;

    /**
     * Remove one or more tax codes.
     * 
     * <p>
     * To delete a single tax code, all arguments should be provided.
     * </p>
     * 
     * @param kbTenantId
     *            the tenant ID
     * @param taxZone
     *            if provided, only delete tax codes matching this tax zone
     * @param productName
     *            if provided, only delete tax codes matching this product name
     * @param taxCode
     *            if provided, only delete tax codes matching this tax code
     * @return the number of deleted tax codes
     * @throws SQLException
     *             if any SQL error occurs
     */
    int removeTaxCodes(final UUID kbTenantId, @Nullable final String taxZone,
            @Nullable final String productName, @Nullable final String taxCode) throws SQLException;

    /**
     * Find all tax codes for a tenant, optionally limited to a specific tax zone, product name, or
     * validity date.
     * 
     * @param kbTenantId
     *            the tenant ID
     * @param taxZone
     *            an optional tax zone to limit the results to
     * @param productName
     *            an optional product name to limit the results to
     * @param taxCode
     *            an optional tax code to limit the results to
     * @param date
     *            an optional validity date to limit the results to
     * @return the found tax codes, never {@literal null}, ordered by the record ID unless
     *         {@code date} provided, then ordered by valid from date in descending order descending
     * @throws SQLException
     *             if any SQL error occurs
     */
    List<EasyTaxTaxCode> getTaxCodes(final UUID kbTenantId, @Nullable final String taxZone,
            @Nullable final String productName, @Nullable String taxCode, @Nullable DateTime date)
            throws SQLException;

    /**
     * Add a taxation record.
     * 
     * <p>
     * When adding tax to an invoice, an additional invoice item is added to the invoice. This
     * record keeps track of the association between the original invoice item (the taxable item)
     * and any generated tax invoice items.
     * </p>
     * 
     * @param taxation
     *            the taxation to save
     * @throws SQLException
     *             if any SQL error occurs
     */
    void addTaxation(EasyTaxTaxation taxation) throws SQLException;

    /**
     * Get the taxation objects for an invoice.
     * 
     * @param kbTenantId
     *            the tenant ID
     * @param kbAccountId
     *            the account ID
     * @param kbInvoiceId
     *            the invoice ID
     * @return the found objects, or an empty list
     * @throws SQLException
     *             if any SQL error occurs
     */
    List<EasyTaxTaxation> getTaxation(final UUID kbTenantId, final UUID kbAccountId,
            final UUID kbInvoiceId) throws SQLException;

}
