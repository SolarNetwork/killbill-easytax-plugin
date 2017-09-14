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

import java.util.Properties;

import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillLogService;
import org.killbill.billing.plugin.api.notification.PluginTenantConfigurableConfigurationHandler;

public class EasyTaxConfigurationHandler
        extends PluginTenantConfigurableConfigurationHandler<EasyTaxConfig> {

    public EasyTaxConfigurationHandler(final String pluginName,
            final OSGIKillbillAPI osgiKillbillApi,
            final OSGIKillbillLogService osgiKillbillLogService) {
        super(pluginName, osgiKillbillApi, osgiKillbillLogService);
    }

    @Override
    protected EasyTaxConfig createConfigurable(final Properties properties) {
        return new EasyTaxConfig(properties);
    }
}
