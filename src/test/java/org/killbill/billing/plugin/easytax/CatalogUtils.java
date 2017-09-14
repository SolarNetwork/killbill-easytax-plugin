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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.killbill.billing.catalog.api.CatalogApiException;
import org.killbill.billing.catalog.api.CatalogUserApi;
import org.killbill.billing.catalog.api.StaticCatalog;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.util.callcontext.TenantContext;

/**
 * Utilities for helping tests dealing with catalogs.
 *
 * @author matt
 */
public class CatalogUtils {

    public static CatalogUserApi createCatalogApi(StaticCatalog currentCatalog)
            throws CatalogApiException {
        CatalogUserApi mock = mock(CatalogUserApi.class);
        if (currentCatalog != null) {
            when(mock.getCurrentCatalog(eq(currentCatalog.getCatalogName()),
                    any(TenantContext.class))).thenReturn(currentCatalog);
        }
        return mock;
    }

    public static void setupCatalogApi(OSGIKillbill mock, CatalogUserApi catalogApi) {
        when(mock.getCatalogUserApi()).thenReturn(catalogApi);
    }

}
