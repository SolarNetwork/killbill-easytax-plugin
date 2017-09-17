EasyTax plugin
==============

Kill Bill tax plugin for simple region bases tax systems. It works by allowing tax rates to be
defined at the following granularity:

 * a **tax zone** that represents a region or group of tax rules, for example a country code like `NZ`
 * a **product** that matches the Kill Bill product name of a taxable invoice item
 * a **tax code** that represents a tax type, for example `GST`
 * a **validity date range** that restricts a tax rate to a specific date range; ranges can be
   defined with a **start** and **end** date or just a **start** date

Based on these attributes, any number of tax rates can be applied to invoice items. The EasyTax
plugin uses configurable tax zone and tax date _resolvers_ to determine the tax zone and tax date to
use for a given invoice item. Any tax rate that matches the resolved attribute values will be
applied and added as tax invoice items.

Kill Bill compatibility
-----------------------

| Plugin version | Kill Bill version |
| -------------: | ----------------: |
| 0.1.y          | 0.18.z            |

Requirements
------------

The plugin needs a database. The latest version of the schema can be found [here](https://github.com/SolarNetwork/killbill-easytax-plugin/blob/master/src/main/resources/ddl.sql).
The `easytax_tax_codes` table holds the tax rates, and can be maintained manually or via the
[REST API](#rest-api) exposed by the plugin. The `easytax_taxations` table is populated by the
plugin itself, and keeps track of which invoice items have been taxed.

Configuration
-------------

The following global properties can be configured; **note** that all properties are prefixed with
`org.killbill.billing.plugin.easytax.`:

 * `taxScale`: the scale to use for calculated tax invoice items; defaults to `2` (see [Tax
   Rounding](#tax-rounding) for more details)

 * `taxRoundingMode`: the `java.math.RoundingMode` to use for calculated tax invoice items: one of
   `CEILING`, `DOWN`, `FLOOR`, `HALF_DOWN`, `HALF_EVEN`, `HALF_UP`, or `UP`; defaults to `HALF_UP`
   (see [Tax Rounding](#tax-rounding) for more details)

 * `taxZoneResolver`: the fully qualified class name that implements the
   `org.killbill.billing.plugin.easytax.api.EasyTaxTaxZoneResolver` API, and is responsible for
   resolving the `taxZone` to use when calculating tax for invoice items; the default is
   `org.killbill.billing.plugin.easytax.core.AccountCustomFieldTaxZoneResolver` which looks for a
   `taxZone` custom field on the _account_ owning the invoice (see [Account Custom Field Tax Zone
   Resolver](#account-custom-field-tax-zone-resolver) for more details)

 * `taxDateResolver`: the fully qualified class name that implements the
   `org.killbill.billing.plugin.easytax.api.EasyTaxTaxDateResolver` API, and is responsible for
   resolving the date to use when calculating tax for invoice items; the default is
   `org.killbill.billing.plugin.easytax.core.SimpleTaxDateResolver` which looks for the end date of
   the invoice item by default (see [Simple Tax Date Resolver](#simple-tax-date-resolver) for more
   details)

See [Account Custom Field Tax Zone  Resolver settings](#account-custom-field-tax-zone-resolver-settings)
and [Simple Tax Date Resolver settings](#simple-tax-date-resolver-settings) for more configuration
details.


### Per tenant configuration

These properties can be specified globally via System Properties or on a per tenant basis:

```
curl -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: text/plain' \
     -d 'org.killbill.billing.plugin.easytax.taxScale = 2
org.killbill.billing.plugin.easytax.taxRoundingMode = HALF_UP
org.killbill.billing.plugin.easytax.taxZoneResolver = org.killbill.billing.plugin.easytax.core.AccountCustomFieldTaxZoneResolver
org.killbill.billing.plugin.easytax.taxDateResolver = org.killbill.billing.plugin.easytax.core.SimpleTaxDateResolver
org.killbill.billing.plugin.easytax.accountCustomFieldTaxZoneResolver.useAccountCountry = true
org.killbill.billing.plugin.easytax.simpleTaxDateResolver.dateMode = EndThenStart
org.killbill.billing.plugin.easytax.simpleTaxDateResolver.defaultTimeZone = UTC
org.killbill.billing.plugin.easytax.simpleTaxDateResolver.fallBackToInvoiceDate = true
org.killbill.billing.plugin.easytax.simpleTaxDateResolver.fallBackToInvoiceItemCreatedDate = true
org.killbill.billing.plugin.easytax.simpleTaxDateResolver.fallBackToInvoiceCreatedDate = true' \
     http://127.0.0.1:8080/1.0/kb/tenants/uploadPluginConfig/killbill-easytax
```

REST API
--------

The plugin exposes a `/plugins/killbill-easytax/taxCodes/{taxZone}/{productName}/{taxCode}` endpoint
for maintaining the tax rates. The path variables are generally optional, and are used to restrict
the action to a more selective set of rates. The endpoint supports the following methods:

 * `GET`: query for a list of available tax rates. A `validNow=true` parameter can also be provided to
   restrict the results to just those whose valid date range includes the current date. A
   `validDate=YYYY-MM-DDTHH:mm:ssZZZ` parameter can also be provided to restrict the results to a
   specific date, using any valid ISO8601 formatted date.
 * `POST`: save one or more tax rates (see format below). A single rate can be added if all path
   variables are provided; otherwise a list of rates is assumed. Rates will be **updated** if the
   **tax zone**, **product name**, **tax code**, and **valid from** dates match, so the _valid to_
   date can be easily adjusted when a tax rate changes.
 * `DELETE`: delete one or more tax rates.

### Authentication

The `DELETE` and `POST` requests require an authenticated user with the `catalog:config_upload`
permission.

### Tax rate JSON syntax

A tax rate object looks like this:

```json
{
  "created_date"    : "2010-10-01T00:00:00.000Z",
  "tenant_id"       : "3e266fd5-50e8-4123-88ed-f9f0f2e6fa16",
  "tax_zone"        : "NZ",
  "product_name"    : "PostedDatumMetrics",
  "tax_code"        : "GST",
  "tax_rate"        : "0.15",
  "valid_from_date" : "2000-10-01T00:00:00.000Z",
  "valid_to_date"   : "2010-10-01T00:00:00.000Z"
}
```

The `*_date` properties are expressed as ISO8601 dates. Only the `valid_to_date` property may be
`null`. The plugin accepts any valid ISO8601 date on requests, including any time zone offset; when
responding the dates will always be formatted in the UTC time zone as shown above.

### Example GET request

To query for tax rates that apply to the _PostedDatumMetrics_ product on 1 Oct 2010
in the _Pacific/Auckland_ time zone, you could make a request like:

```
curl -X GET \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
	'http://127.0.0.1:8080/plugins/killbill-easytax/taxCodes/NZ/PostedDatumMetrics?validDate=2010-10-01T00:00%2B13:00'
```

which would return results like:

```json
[
  {
    "created_date": "2017-09-14T05:33:27.000Z",
    "tenant_id": "3e266fd5-50e8-4123-88ed-f9f0f2e6fa16",
    "tax_zone": "NZ",
    "product_name": "PostedDatumMetrics",
    "tax_code": "GST",
    "tax_rate": "0.150000000",
    "valid_from_date": "2010-09-30T11:00:00.000Z"
  }
]
```

Note how the `validDate` query parameter was expressed in _Pacific/Auckland_ time, while the
response is in UTC. Also note the response does not include a `valid_to_date`, which indicates that
tax rate is in effect for any date on or after the `valid_from_date`.

You could also use the `?validNow=true` request parameter as a shortcut for finding the effective
tax rates for the current time.

### Example POST request

To upload tax rates in bulk, a request like the following could be used to represent a single tax
code `GST` on a `PostedDatumMetrics` product whose rate changed on 1 Oct 2010 from 12.5% to 15% and
has not changed since then:


```
curl -X POST \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
     -H 'Content-Type: application/json; charset=utf-8' \
     -d $'[
  {
    "tax_zone": "NZ",
    "product_name": "PostedDatumMetrics",
    "tax_code": "GST",
    "tax_rate": "0.125",
    "valid_from_date": "1999-01-01T00:00:00+13:00",
    "valid_to_date": "2010-10-01T00:00:00+13:00"
  },
  {
    "tax_zone": "NZ",
    "product_name": "PostedDatumMetrics",
    "tax_code": "GST",
    "tax_rate": "0.15",
    "valid_from_date": "2010-10-01T00:00:00+13:00"
  }
]' \
	'http://127.0.0.1:8080/plugins/killbill-easytax/taxCodes'
```

### Example DELETE request

To delete all tax rates that apply to the _PostedDatumMetrics_ product in the _NZ_ tax zone, you
could make a request like:

```
curl -X DELETE \
     -u admin:password \
     -H 'X-Killbill-ApiKey: bob' \
     -H 'X-Killbill-ApiSecret: lazar' \
     -H 'X-Killbill-CreatedBy: admin' \
	'http://127.0.0.1:8080/plugins/killbill-easytax/taxCodes/NZ/PostedDatumMetrics'
```


EasyTax tax calculation details
-------------------------------

When this plugin examines an invoice, it looks for all invoice items that could be taxed (for
example are not already tax items) and attempts to resolve a set of tax rates to apply to those
items. To resolve the tax rates, it must determine the following attributes to match against the
`easytax_tax_codes` table:

 1. **tax zone** - compared to the `tax_zone` column
 2. **product name** - compared to the `product_name` column
 3. **tax date** - compared as _greater or equal_ than the `valid_from_date` and _less than_ the
    `valid_to_date` column; a `NULL` `valid_to_date` is considered as infinitely in the future

Once the tax zone, product name, and tax date are resolved for an invoice item, the plugin queries
the `easytax_tax_codes` table for **all** matching records, and adds new tax invoice items for each
record, using the associated `tax_rate` values.

### Tax rounding

When calculating the tax values, the results are rounded using the `taxScale` and `taxRoundingMode`
configuration properties. The **scale** value determines how many places after the value's decimal
point to round to, and the **rounding mode** determines the rounding rule to use. The default
settings used by EasyTax are:

| Setting | Default |
| ------- | ------- |
| scale   | 2       |
| mode    | HALF_UP |

The rounding modes are taken directly from the [`java.math.RoundingMode`][RoundingMode] class.

### Account Custom Field Tax Zone Resolver

This tax zone resolver looks for a `taxZone` custom field set on the **account** that owns the
invoice. If found, that value will be used as the tax zone when calculating tax. If a custom field
is not found, and the `easyTaxTaxZoneResolver.useAccountCountry` configuration property is `true`
(the default value), then this resolver will use the **country** of the account that owns the
invoice.

#### Account Custom Field Tax Zone Resolver settings

 * `accountCustomFieldTaxZoneResolver.useAccountCountry`: boolean flag to fall back to using an
   account's country as a tax zone, if no `taxZone` custom field is available


### Simple Tax Date Resolver

This tax date resolver can be configured based using a **date mode** that specifies what date to
use for invoice items. Consider a subscription item with a start and end date -- the date mode
determines which one to use for the purposes of finding the tax rates to apply. The resolver
can then fall back to other dates when invoice items don't have the preferred date.

| Date Mode    | Description                                              |
| ------------ | -------------------------------------------------------- |
| End          | Use the item's end date.                                 |
| EndThenStart | Use the item's end date, falling back to the start date. |
| Invoice      | Use the invoice date.                                    |
| Start        | Use the item's start date.                               |
| StartThenEnd | Use the item's start date, falling back to the end date. |

If a date cannot be resolved using the date mode, then there are three more fallback options
available, which are considered **in the following order** such that the first available
date is used:

 | Fallback                   | Description                            |
 | -------------------------- | -------------------------------------- |
 | Invoice date               | Use the invoice date.                  |
 | Invoice item creation date | The creation date of the invoice item. |
 | Invoice creation date      | The creation date of the invoice.      |
 | Current date               | The current date of the system.        |

The item start/end and invoice dates are expressed as _local_ dates, and thus will be
resolved into an absolute date using the **time zone** of the **account** that owns the
invoice. If a time zone is not available on the account, a fallback will be used (see
the `simpleTaxDateResolver.defaultTimeZone` below).

#### Simple Tax Date Resolver settings

 * `simpleTaxDateResolver.dateMode`: the date mode to use, as outlined in the table above;
   defaults to `EndThenStart`

 * `simpleTaxDateResolver.defaultTimeZone`: a default time zone to use for resolving the
   item start, item end, and invoice dates into absolute dates if the associated account
   does not have a time zone available; defaults to `UTC`

 * `simpleTaxDateResolver.fallBackToInvoiceDate`: boolean flag to allow falling back to
   using the invoice date if the date mode rules don't resolve a date

 * `simpleTaxDateResolver.fallBackToInvoiceItemCreatedDate`: boolean flag to allow
   falling back to using the invoice item creation date if the date mode rules don't resolve
   a date

 * `simpleTaxDateResolver.fallBackToInvoiceItemCreatedDate`: boolean flag to allow
   falling back to using the invoice item creation date if the date mode rules don't resolve
   a date

 * `simpleTaxDateResolver.fallBackToInvoiceCreatedDate`: boolean flag to allow falling
   back to using the invoice creation date if the date mode rules don't resolve a date

Releasing
---------

A command like the following is used to release the plugin:

```
mvn -Pjdk18 release:prepare -DreleaseProfiles=jdk18 -Darguments=-Dgpg.keyname=4BC94956
```

The GPG key used for signing is available on the [MIT public server][keyserver].


 [RoundingMode]: https://docs.oracle.com/javase/8/docs/api/java/math/RoundingMode.html
 [keyserver]: https://pgp.mit.edu/pks/lookup?search=%40solarnetwork.net&op=index
