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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.catalog.api.CatalogApiException;
import org.killbill.billing.catalog.api.Plan;
import org.killbill.billing.catalog.api.StaticCatalog;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.osgi.api.OSGIKillbill;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginTaxCalculator;
import org.killbill.billing.plugin.easytax.core.AccountCustomFieldTaxZoneResolver;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfig;
import org.killbill.billing.plugin.easytax.core.EasyTaxConfigurationHandler;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxCode;
import org.killbill.billing.plugin.easytax.core.EasyTaxTaxation;
import org.killbill.billing.plugin.easytax.core.SimpleTaxDateResolver;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PluginTaxCalculator} that applies tax rates based on {@link EasyTaxTaxCode} entities
 * loaded in the database.
 * 
 * @author matt
 * @version 2
 */
public class EasyTaxTaxCalculator extends PluginTaxCalculator {

    /** The service filter property key for the tenant ID. */
    public static final String TENANT_ID_FILTER = "tenant";

    // CHECKSTYLE OFF: LineLength
    private static final ConcurrentMap<UUID, EasyTaxTaxZoneResolver> ZONE_RESOLVER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, EasyTaxTaxDateResolver> DATE_RESOLVER_CACHE = new ConcurrentHashMap<>();
    // CHECKSTYLE ON: LineLength

    private final OSGIKillbill killbillApi;
    private final EasyTaxConfigurationHandler configurationHandler;
    private final EasyTaxDao dao;
    private final OptionalService<EasyTaxTaxZoneResolver> taxZoneResolver;
    private final OptionalService<EasyTaxTaxDateResolver> taxDateResolver;
    private final Clock clock;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor.
     * 
     * @param killbillApi
     *            the Killbill API to use
     * @param configurationHandler
     *            the configuration handler to use
     * @param dao
     *            the DAO to use
     * @param taxZoneResolver
     *            the tax zone resolver service
     * @param taxDateResolver
     *            the tax date resolver service
     * @param clock
     *            the system clock
     */
    public EasyTaxTaxCalculator(final OSGIKillbill killbillApi,
            final EasyTaxConfigurationHandler configurationHandler, final EasyTaxDao dao,
            final OptionalService<EasyTaxTaxZoneResolver> taxZoneResolver,
            final OptionalService<EasyTaxTaxDateResolver> taxDateResolver, final Clock clock) {
        super();
        this.killbillApi = killbillApi;
        this.configurationHandler = configurationHandler;
        this.dao = dao;
        this.taxZoneResolver = taxZoneResolver;
        this.taxDateResolver = taxDateResolver;
        this.clock = clock;
    }

    /**
     * Get an {@link EasyTaxTaxZoneResolver} to use.
     * 
     * <p>
     * This will use the configured {@code OptionalService<EasyTaxTaxZoneResolver>} first, passing a
     * filter on {@link #TENANT_ID_FILTER} that matches the given tenant ID or the absence of a
     * tenant ID (e.g. a global service). If no service is found that way, the
     * {@link EasyTaxConfig#getTaxZoneResolver()} property is used to instantiate a resolver for the
     * given tenant.
     * </p>
     * 
     * @param kbTenantId
     *            the tenant ID
     * @return the resolver to use
     */
    private EasyTaxTaxZoneResolver taxZoneResolver(UUID kbTenantId) {
        EasyTaxTaxZoneResolver result = taxZoneResolver.service(
                OptionalService.equalOrAbsentFilter(TENANT_ID_FILTER, kbTenantId.toString()));
        if (result != null) {
            return result;
        }
        return ZONE_RESOLVER_CACHE.computeIfAbsent(kbTenantId, k -> {
            EasyTaxConfig config = configurationHandler.getConfigurable(kbTenantId);
            String resolverClassName = config.getTaxZoneResolver();
            EasyTaxTaxZoneResolver resolover = null;
            try {
                resolover = (EasyTaxTaxZoneResolver) EasyTaxTaxCalculator.class.getClassLoader()
                        .loadClass(resolverClassName).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                log.error("Error instantiating EasyTaxTaxZoneResolver class [{}]; using default",
                        resolverClassName, e);
                resolover = new AccountCustomFieldTaxZoneResolver();
            }
            resolover.init(killbillApi, config);
            return resolover;
        });
    }

    /**
     * Get an {@link EasyTaxTaxDateResolver} to use.
     * 
     * <p>
     * This will use the configured {@code OptionalService<EasyTaxTaxDateResolver>} first, passing a
     * filter on {@link #TENANT_ID_FILTER} that matches the given tenant ID or the absence of a
     * tenant ID (e.g. a global service). If no service is found that way, the
     * {@link EasyTaxConfig#getTaxDateResolver()} property is used to instantiate a resolver for the
     * given tenant.
     * </p>
     * 
     * @param kbTenantId
     *            the tenant ID
     * @return the resolver to use
     */
    private EasyTaxTaxDateResolver taxDateResolver(UUID kbTenantId) {
        EasyTaxTaxDateResolver result = taxDateResolver.service(
                OptionalService.equalOrAbsentFilter(TENANT_ID_FILTER, kbTenantId.toString()));
        if (result != null) {
            return result;
        }
        return DATE_RESOLVER_CACHE.computeIfAbsent(kbTenantId, k -> {
            EasyTaxConfig config = configurationHandler.getConfigurable(kbTenantId);
            String resolverClassName = config.getTaxDateResolver();
            EasyTaxTaxDateResolver resolver = null;
            try {
                resolver = (EasyTaxTaxDateResolver) EasyTaxTaxCalculator.class.getClassLoader()
                        .loadClass(resolverClassName).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                log.error("Error instantiating EasyTaxTaxDateResolver class [{}]; using default",
                        resolverClassName, e);
                resolver = new SimpleTaxDateResolver();
            }
            resolver.init(killbillApi, config);
            return resolver;
        });
    }

    @Override
    public List<InvoiceItem> compute(Account account, Invoice newInvoice, Invoice invoice,
            Map<UUID, InvoiceItem> taxableItems, Map<UUID, Collection<InvoiceItem>> adjustmentItems,
            boolean dryRun, Iterable<PluginProperty> pluginProperties, UUID kbTenantId) {
        // instantiate zone resolver
        EasyTaxTaxZoneResolver resolver = taxZoneResolver(kbTenantId);
        String taxZone = resolver.taxZoneForInvoice(kbTenantId, account, invoice, pluginProperties);
        if (taxZone == null) {
            return Collections.emptyList();
        }

        Map<UUID, Set<UUID>> alreadyTaxedItems = getAlreadyTaxedItemsWithTaxes(invoice, kbTenantId);

        final Map<UUID, InvoiceItem> salesTaxItems = new LinkedHashMap<>();
        final Map<UUID, InvoiceItem> returnTaxItems = new LinkedHashMap<>();
        // CHECKSTYLE OFF: LineLength
        final Map<UUID, Collection<InvoiceItem>> adjustmentItemsForReturnTaxItems = new LinkedHashMap<>();
        computeNewItemsToTaxAndExistingItemsToAdjust(taxableItems, adjustmentItems,
                alreadyTaxedItems, salesTaxItems, returnTaxItems, adjustmentItemsForReturnTaxItems);
        // CHECKSTYLE ON: LineLength
        // TODO: make static for actual caching support?
        final Map<String, String> planToProductCache = new HashMap<>();

        List<InvoiceItem> newTaxInvoiceItems = new ArrayList<>();
        if (!salesTaxItems.isEmpty()) {
            newTaxInvoiceItems.addAll(getTaxItems(account, newInvoice, invoice, salesTaxItems, null,
                    null, dryRun, taxZone, planToProductCache, kbTenantId));
        }
        if (!returnTaxItems.isEmpty()) {
            // TODO: tracking original invoice ref code?
            final String originalInvoiceReferenceCode;
            try {
                final List<EasyTaxTaxation> responses = dao.getTaxation(kbTenantId, account.getId(),
                        invoice.getId());
                originalInvoiceReferenceCode = responses.isEmpty() ? null
                        : responses.get(0).getRecordId().toString();
            } catch (final SQLException e) {
                log.warn("Unable to compute tax for account " + account.getId(), e);
                return Collections.emptyList();
            }

            newTaxInvoiceItems.addAll(getTaxItems(account, newInvoice, invoice, returnTaxItems,
                    adjustmentItemsForReturnTaxItems, originalInvoiceReferenceCode, dryRun, taxZone,
                    planToProductCache, kbTenantId));
        }

        // add to already taxed settings
        if (!newTaxInvoiceItems.isEmpty()) {
            EasyTaxTaxation taxation = new EasyTaxTaxation();
            taxation.setCreatedDate(clock.getUTCNow());
            taxation.setKbTenantId(kbTenantId);
            taxation.setKbAccountId(account.getId());
            taxation.setKbInvoiceId(invoice.getId());

            // sum up total new tax and update the mapping of taxable -> tax item IDs
            Map<UUID, Set<UUID>> taxedItemsWithAdjustments = new HashMap<>();
            for (Map.Entry<UUID, Collection<InvoiceItem>> entry : adjustmentItems.entrySet()) {
                taxedItemsWithAdjustments.put(entry.getKey(), entry.getValue().stream()
                        .map(item -> item.getId()).collect(Collectors.toSet()));
            }
            BigDecimal totalTax = BigDecimal.ZERO;
            for (InvoiceItem item : newTaxInvoiceItems) {
                totalTax = totalTax.add(item.getAmount());
                taxedItemsWithAdjustments
                        .computeIfAbsent(item.getLinkedItemId(), k -> new HashSet<>())
                        .add(item.getId());
            }
            taxation.setInvoiceItemIds(taxedItemsWithAdjustments);
            taxation.setTotalTax(totalTax);
            try {
                dao.addTaxation(taxation);
            } catch (SQLException e) {
                log.error("Error saving taxation record for invoice {}", invoice.getId(), e);
                return Collections.emptyList();
            }
        }

        return Collections.unmodifiableList(newTaxInvoiceItems);

    }

    /**
     * Get a mapping of existing taxable invoice item IDs to associated tax invoice items on an
     * invoice.
     * 
     * @param invoice
     *            the invoice to inspect
     * @param kbTenantId
     *            the tenant ID
     * @return the mapping, or an empty {@code Map} if no existing items available
     */
    private Map<UUID, Set<UUID>> getAlreadyTaxedItemsWithTaxes(Invoice invoice, UUID kbTenantId) {
        Map<UUID, Set<UUID>> alreadyTaxed = null;
        try {
            List<EasyTaxTaxation> taxations = dao.getTaxation(kbTenantId, invoice.getAccountId(),
                    invoice.getId());
            if (!taxations.isEmpty()) {
                if (taxations.size() == 1) {
                    alreadyTaxed = taxations.get(0).getInvoiceItemIds();
                } else {
                    alreadyTaxed = new HashMap<>();
                    for (EasyTaxTaxation taxation : taxations) {
                        for (Map.Entry<UUID, Set<UUID>> entry : taxation.getInvoiceItemIds()
                                .entrySet()) {
                            alreadyTaxed.computeIfAbsent(entry.getKey(), k -> new HashSet<>())
                                    .addAll(entry.getValue());
                        }
                    }
                }
            }
        } catch (final SQLException e) {
            log.warn("Unable to compute tax for account {}", invoice.getAccountId(), e);
        }
        return alreadyTaxed != null ? alreadyTaxed : Collections.emptyMap();
    }

    private List<InvoiceItem> getTaxItems(final Account account, final Invoice newInvoice,
            final Invoice invoice, final Map<UUID, InvoiceItem> taxableItems,
            @Nullable final Map<UUID, Collection<InvoiceItem>> adjustmentItems,
            @Nullable final String originalInvoiceReferenceCode, final boolean dryRun,
            final String taxZone, final Map<String, String> planToProductCache,
            final UUID kbTenantId) {
        // Keep track of the invoice items and adjustments we've already taxed
        final Map<UUID, Iterable<InvoiceItem>> kbInvoiceItems = new HashMap<>();
        if (adjustmentItems != null) {
            kbInvoiceItems.putAll(adjustmentItems);
        }
        for (final InvoiceItem taxableItem : taxableItems.values()) {
            if (kbInvoiceItems.get(taxableItem.getId()) == null) {
                kbInvoiceItems.put(taxableItem.getId(), Collections.emptyList());
            }
        }
        // Don't use clock.getUTCToday(), see https://github.com/killbill/killbill-platform/issues/4
        final LocalDate taxItemsDate = newInvoice.getInvoiceDate();

        try {
            return buildInvoiceItems(account, newInvoice, invoice, taxableItems, adjustmentItems,
                    originalInvoiceReferenceCode, dryRun, taxZone, planToProductCache, kbTenantId,
                    kbInvoiceItems, taxItemsDate);
        } catch (final RuntimeException e) {
            log.warn("Unable to compute tax for account " + account.getId(), e);
            return Collections.emptyList();
        } catch (final SQLException e) {
            log.warn("Unable to compute tax for account " + account.getId(), e);
            return Collections.emptyList();
        }
    }

    private String productNameForInvoiceItem(final InvoiceItem invoiceItem,
            final Map<String, String> planToProductCache, final UUID kbTenantId) {
        final String planName = invoiceItem.getPlanName();
        if (planName == null) {
            return null;
        }

        return planToProductCache.computeIfAbsent(planName, k -> {
            try {
                StaticCatalog catalog = killbillApi.getCatalogUserApi().getCurrentCatalog(null,
                        new EasyTaxTenantContext(kbTenantId, invoiceItem.getAccountId()));
                Plan plan = catalog.findPlan(planName);
                return (plan != null && plan.getProduct() != null ? plan.getProduct().getName()
                        : null);
            } catch (CatalogApiException e) {
                return null;
            }
        });
    }

    private List<InvoiceItem> buildInvoiceItems(final Account account, final Invoice newInvoice,
            final Invoice invoice, final Map<UUID, InvoiceItem> taxableItems,
            @Nullable final Map<UUID, Collection<InvoiceItem>> adjustmentItems,
            @Nullable final String originalInvoiceReferenceCode, final boolean dryRun,
            final String taxZone, final Map<String, String> planToProductCache,
            final UUID kbTenantId, final Map<UUID, Iterable<InvoiceItem>> kbInvoiceItems,
            final LocalDate utcToday) throws SQLException {
        final List<InvoiceItem> newTaxItems = new ArrayList<>();
        for (final InvoiceItem taxableItem : taxableItems.values()) {
            final Collection<InvoiceItem> adjustmentsForTaxableItem = adjustmentItems == null ? null
                    : adjustmentItems.get(taxableItem.getId());
            final BigDecimal netItemAmount = adjustmentsForTaxableItem == null
                    ? taxableItem.getAmount()
                    : sum(adjustmentsForTaxableItem);
            newTaxItems.addAll(taxInvoiceItemsForInvoiceItem(account, newInvoice, taxableItem,
                    taxZone, netItemAmount, utcToday, kbTenantId, planToProductCache));
        }

        return newTaxItems;
    }

    private DateTime taxDateForInvoiceItem(UUID kbTenantId, Account account, Invoice invoice,
            InvoiceItem item) {
        EasyTaxTaxDateResolver dateResolver = taxDateResolver(kbTenantId);
        return dateResolver.taxDateForInvoiceItem(kbTenantId, account, invoice, item, null);
    }

    private List<InvoiceItem> taxInvoiceItemsForInvoiceItem(final Account account,
            final Invoice newInvoice, final InvoiceItem taxableItem, final String taxZone,
            final BigDecimal netItemAmount, final LocalDate utcToday, final UUID kbTenantId,
            final Map<String, String> planToProductCache) throws SQLException {
        DateTime taxDate = taxDateForInvoiceItem(kbTenantId, account, newInvoice, taxableItem);
        if (taxDate == null) {
            // use the current date; should this be configurable (i.e. to bail if not found)?
            taxDate = clock.getUTCNow();
        }
        String productName = productNameForInvoiceItem(taxableItem, planToProductCache, kbTenantId);
        List<EasyTaxTaxCode> taxCodes = dao.getTaxCodes(kbTenantId, taxZone, productName, null,
                taxDate);
        if (taxCodes == null || taxCodes.isEmpty()) {
            return Collections.emptyList();
        }
        final EasyTaxConfig config = configurationHandler.getConfigurable(kbTenantId);
        final int scale = config.getTaxScale();
        final RoundingMode roundingMode = config.getTaxRoundingMode();
        List<InvoiceItem> newTaxItems = new ArrayList<>();
        for (EasyTaxTaxCode taxCode : taxCodes) {
            InvoiceItem taxItem = buildTaxItem(taxableItem, newInvoice.getId(), utcToday,
                    taxCode.getTaxRate().multiply(netItemAmount).setScale(scale, roundingMode),
                    taxCode.getTaxCode());
            if (taxItem != null) {
                newTaxItems.add(taxItem);
            }
        }
        return newTaxItems;
    }
}
