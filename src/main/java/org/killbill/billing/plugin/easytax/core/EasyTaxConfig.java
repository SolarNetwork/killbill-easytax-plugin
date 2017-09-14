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

import java.math.RoundingMode;
import java.util.Properties;

import javax.annotation.Nullable;

import org.killbill.billing.plugin.easytax.api.EasyTaxTaxDateResolver;
import org.killbill.billing.plugin.easytax.api.EasyTaxTaxZoneResolver;

/**
 * Configurable properties for the EasyTax plugin.
 * 
 * @author matt
 */
public class EasyTaxConfig {

    /** The prefix used for all configuration properties. */
    public static final String PROPERTY_PREFIX = "org.killbill.billing.plugin.easytax.";

    /** The configuration property for the tax zone resolver class name. */
    public static final String TAX_ZONE_RESOLVER_PROPERTY = "taxZoneResolver";

    /** The configuration property for the tax date resolver class name. */
    public static final String TAX_DATE_RESOLVER_PROPERTY = "taxDateResolver";

    /** The configuration property for the numeric scale to round calculated amounts to. */
    public static final String TAX_SCALE_PROPERTY = "taxScale";

    /** The configuration property for the tax rounding method to use. */
    public static final String TAX_ROUNDING_MODE_PROPERTY = "taxRoundingMode";

    /**
     * The default value for the {@code taxScale} property.
     */
    public static final String DEFAULT_TAX_SCALE = "2";

    /**
     * The default value for the {@code taxRoundingMode} configuration property.
     */
    public static final String DEFAULT_TAX_ROUNDING_MODE = RoundingMode.HALF_UP.name();

    /**
     * The default {@link EasyTaxTaxZoneResolver} class name.
     */
    public static final String DEFAULT_TAX_ZONE_RESOLVER = AccountCustomFieldTaxZoneResolver.class
            .getName();

    /**
     * The default {@link EasyTaxTaxDateResolver} class name.
     */
    public static final String DEFAULT_TAX_DATE_RESOLVER = SimpleTaxDateResolver.class.getName();

    private final Properties properties;

    public EasyTaxConfig(final Properties properties) {
        super();
        this.properties = properties;
    }

    /**
     * Get the name of the {@link EasyTaxTaxZoneResolver} class to use.
     * 
     * <p>
     * This returns the {@link #TAX_ZONE_RESOLVER_PROPERTY}.
     * </p>
     * 
     * @return the name of the tax zone resolver class to use
     */
    public String getTaxZoneResolver() {
        return properties.getProperty(PROPERTY_PREFIX + TAX_ZONE_RESOLVER_PROPERTY,
                DEFAULT_TAX_ZONE_RESOLVER);
    }

    /**
     * Get the name of the {@link EasyTaxTaxZoneResolver} class to use.
     * 
     * <p>
     * This returns the {@link #TAX_ZONE_RESOLVER_PROPERTY}.
     * </p>
     * 
     * @return the name of the tax zone resolver class to use
     */
    public String getTaxDateResolver() {
        return properties.getProperty(PROPERTY_PREFIX + TAX_DATE_RESOLVER_PROPERTY,
                DEFAULT_TAX_DATE_RESOLVER);
    }

    /**
     * Get the tax rounding mode to use.
     * 
     * <p>
     * This returns the {@link #TAX_ROUNDING_MODE_PROPERTY}. Defaults to {@literal HALF_UP}.
     * </p>
     * 
     * @return the rounding mode
     */
    public RoundingMode getTaxRoundingMode() {
        String mode = getConfigurationValue(TAX_ROUNDING_MODE_PROPERTY, DEFAULT_TAX_ROUNDING_MODE);
        RoundingMode result;
        try {
            result = RoundingMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            result = RoundingMode.HALF_UP;
        }
        return result;
    }

    /**
     * Get the tax scale to use when rounding.
     * 
     * <p>
     * This returns the {@link #TAX_SCALE_PROPERTY}. Defaults to {@literal 2}.
     * </p>
     * 
     * @return the tax scale
     */
    public int getTaxScale() {
        String scale = getConfigurationValue(TAX_SCALE_PROPERTY, DEFAULT_TAX_SCALE);
        int result;
        try {
            result = Integer.parseInt(scale);
        } catch (NumberFormatException e) {
            result = 2;
        }
        return result;
    }

    /**
     * Get a general configuration value.
     * 
     * @param key
     *            the key, which should not include {@link EasyTaxConfig#PROPERTY_PREFIX}
     * @param defaultValue
     *            a default value to use if the property is not available
     * @return the discovered value, or {@code defaultValue} if not available
     */
    public String getConfigurationValue(String key, @Nullable String defaultValue) {
        return properties.getProperty(PROPERTY_PREFIX + key, defaultValue);
    }

}
