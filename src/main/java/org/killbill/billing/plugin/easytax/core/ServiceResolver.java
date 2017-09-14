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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.killbill.billing.plugin.easytax.api.OptionalService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link OptionalService} implementation that uses OSGi to resolve services at runtime.
 * 
 * @author matt
 */
public class ServiceResolver<T> implements OptionalService<T> {

    private BundleContext bundleContext;

    private Class<T> serviceClass;
    private String serviceFilter;
    private T fallbackService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor.
     * 
     * @param context
     *            the bundle context
     * @param serviceClass
     *            the class of the service to resolve
     * @param fallbackService
     *            a service to default to if no other one is available at runtime
     */
    public ServiceResolver(BundleContext context, Class<T> serviceClass, T fallbackService) {
        this.bundleContext = context;
        this.serviceClass = serviceClass;
        this.fallbackService = fallbackService;
    }

    /**
     * Constructor.
     * 
     * @param context
     *            the bundle context
     * @param filter
     *            the service filter
     * @param fallbackService
     *            a service to default to if no other one is available at runtime
     */
    public ServiceResolver(BundleContext context, String filter, T fallbackService) {
        super();
        this.bundleContext = context;
        this.serviceFilter = filter;
        this.fallbackService = fallbackService;
    }

    private String computeServiceFilter(String filter) {
        if ((filter == null || filter.length() < 1)
                && (serviceFilter == null || serviceFilter.length() < 1)) {
            return null;
        }
        if (serviceFilter == null || serviceFilter.length() < 1) {
            return filter;
        }
        if (filter == null || filter.length() < 1) {
            return serviceFilter;
        }
        return "(&" + filter + serviceFilter + ")";
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public T service(String filter) {
        String finalFilter = computeServiceFilter(filter);
        Collection<ServiceReference<T>> refs;
        try {
            if (serviceClass != null) {
                refs = bundleContext.getServiceReferences(serviceClass, finalFilter);
            } else {
                ServiceReference<?>[] found = bundleContext.getAllServiceReferences(null,
                        finalFilter);
                if (found != null) {
                    refs = (List) Arrays.asList(found);
                } else {
                    refs = Collections.emptyList();
                }
            }
        } catch (InvalidSyntaxException e) {
            log.error("Error in service filter {}: {}", serviceFilter, e);
            return fallbackService;
        }
        log.debug("Found {} possible services of type {} matching filter {}", refs.size(),
                serviceClass, serviceFilter);
        if (refs.isEmpty()) {
            for (ServiceReference<T> ref : refs) {
                T service = bundleContext.getService(ref);
                if (service != null) {
                    return service;
                }
            }
        }
        if (fallbackService != null) {
            log.debug("No {} service found, using fallback service {}",
                    serviceClass != null ? serviceClass.getName() : serviceFilter,
                    fallbackService.getClass().getName());
        }
        return fallbackService;
    }

}
