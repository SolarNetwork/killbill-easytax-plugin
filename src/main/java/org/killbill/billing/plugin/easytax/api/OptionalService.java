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

/**
 * Simplified API for resolving a service at runtime.
 * 
 * @author matt
 */
public interface OptionalService<T> {

    /**
     * Get the configured service.
     * 
     * @param filter
     *            an optional LDAP style filter
     * @return the service, or {@literal null} if not available
     */
    T service(String filter);

    /**
     * Helper to construct a filter to match a property value or the absence of that property.
     * 
     * @param key
     *            the property name to match
     * @param value
     *            the property value to match
     * @return the filter string
     */
    static String equalOrAbsentFilter(String key, String value) {
        return "(|" + equalFilter(key, value) + "(!(" + key + "=*)))";
    }

    /**
     * Helper to construct a filter to match a property value.
     * 
     * @param key
     *            the property name to match
     * @param value
     *            the property value to match
     * @return the filter string
     */
    static String equalFilter(String key, String value) {
        return "(" + key + "=" + value + ")";
    }

}
