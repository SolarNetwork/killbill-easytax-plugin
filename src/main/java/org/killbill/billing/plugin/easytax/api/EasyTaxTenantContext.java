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

import java.util.UUID;

import org.killbill.billing.util.callcontext.TenantContext;

/**
 * Tenant ID for EasyTax.
 * 
 * @author matt
 */
public class EasyTaxTenantContext implements TenantContext {

    private UUID tenantId;
    private UUID accountId;

    /**
     * Constructor.
     * 
     * @param tenantId
     *            the tenant ID
     * @param accountId
     *            the account ID
     */
    public EasyTaxTenantContext(UUID tenantId, UUID accountId) {
        super();
        this.tenantId = tenantId;
        this.accountId = accountId;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Get the account ID.
     * 
     * @return the account ID
     */
    public UUID getAccountId() {
        return accountId;
    }

}
