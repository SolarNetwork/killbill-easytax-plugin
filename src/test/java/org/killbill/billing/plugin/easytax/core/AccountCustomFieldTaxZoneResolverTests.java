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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import org.joda.time.DateTimeZone;
import org.killbill.billing.ObjectType;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.plugin.easytax.EasyTaxTestUtils;
import org.killbill.billing.util.api.CustomFieldUserApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.customfield.CustomField;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the {@link AccountCustomFieldTaxZoneResolver} class.
 * 
 * @author matt
 */
public class AccountCustomFieldTaxZoneResolverTests {

    private static final String TEST_TAX_ZONE = "test.zone";

    private OSGIKillbill killbillApi;
    private UUID tenantId;

    @BeforeMethod(alwaysRun = true)
    public void setup() {
        tenantId = UUID.randomUUID();
        killbillApi = Mockito.mock(OSGIKillbill.class);
    }

    @AfterTest
    public void teardown() {
        Mockito.validateMockitoUsage();
    }

    private AccountCustomFieldTaxZoneResolver createResolver(Properties properties) {
        AccountCustomFieldTaxZoneResolver resolver = new AccountCustomFieldTaxZoneResolver();
        resolver.init(killbillApi, new EasyTaxConfig(properties));
        return resolver;
    }

    private CustomField createCustomField(String name, String value) {
        CustomField field = Mockito.mock(CustomField.class);
        when(field.getFieldName()).thenReturn(name);
        when(field.getFieldValue()).thenReturn(value);
        return field;
    }

    @Test(groups = "fast")
    public void defaultAccountCustomField() {
        // given
        Account account = EasyTaxTestUtils.createAccount("NZ",
                DateTimeZone.forID("Pacific/Auckland"));
        CustomFieldUserApi fieldApi = Mockito.mock(CustomFieldUserApi.class);
        when(killbillApi.getCustomFieldUserApi()).thenReturn(fieldApi);
        ArgumentCaptor<TenantContext> contextCaptor = ArgumentCaptor.forClass(TenantContext.class);
        CustomField customField = createCustomField(
                AccountCustomFieldTaxZoneResolver.TAX_ZONE_CUSTOM_FIIELD, TEST_TAX_ZONE);
        when(fieldApi.getCustomFieldsForObject(eq(account.getId()), eq(ObjectType.ACCOUNT),
                contextCaptor.capture())).thenReturn(Collections.singletonList(customField));

        // when
        AccountCustomFieldTaxZoneResolver resolver = createResolver(new Properties());
        String taxZone = resolver.taxZoneForInvoice(tenantId, account, null,
                Collections.emptyList());

        // then
        assertEquals(contextCaptor.getValue().getTenantId(), tenantId, "TenantContext tenant ID");
        assertEquals(taxZone, TEST_TAX_ZONE, "Tax zone resolved from account custom field");
    }

    @Test(groups = "fast")
    public void defaultFallbackToAccountCountry() {
        // given
        Account account = EasyTaxTestUtils.createAccount("NZ",
                DateTimeZone.forID("Pacific/Auckland"));
        CustomFieldUserApi fieldApi = Mockito.mock(CustomFieldUserApi.class);
        when(killbillApi.getCustomFieldUserApi()).thenReturn(fieldApi);
        ArgumentCaptor<TenantContext> contextCaptor = ArgumentCaptor.forClass(TenantContext.class);
        when(fieldApi.getCustomFieldsForObject(eq(account.getId()), eq(ObjectType.ACCOUNT),
                contextCaptor.capture())).thenReturn(Collections.emptyList());

        // when
        AccountCustomFieldTaxZoneResolver resolver = createResolver(new Properties());
        String taxZone = resolver.taxZoneForInvoice(tenantId, account, null,
                Collections.emptyList());

        // then
        assertEquals(contextCaptor.getValue().getTenantId(), tenantId, "TenantContext tenant ID");
        assertEquals(taxZone, account.getCountry(),
                "Fallback to account country when no custom field present");
    }

}
