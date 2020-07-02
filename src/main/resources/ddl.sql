/*! SET default_storage_engine=INNODB */;

/* NOTE record_id serial primary key suffers from MySQL bug
 * https://bugs.mysql.com/bug.php?id=37130
 */

SELECT @@global.time_zone, @@session.time_zone;

drop table if exists easytax_taxations;
create table easytax_taxations (
  record_id serial
, kb_tenant_id char(36) not null
, kb_account_id char(36) not null
, kb_invoice_id char(36) not null
, kb_invoice_item_ids mediumtext default null
, total_tax numeric(15,9) default null
, created_date datetime not null
, primary key(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
create index easytax_taxations_account_invoice_idx on easytax_taxations(kb_account_id, kb_invoice_id);

drop table if exists easytax_tax_codes;
create table easytax_tax_codes (
  record_id serial
, kb_tenant_id char(36) not null
, tax_zone varchar(36) not null
, product_name varchar(255) not null
, tax_code varchar(255) not null
, tax_rate numeric(15,9) not null
, valid_from_date datetime not null
, valid_to_date datetime
, created_date datetime not null
, primary key(record_id)
) /*! CHARACTER SET utf8 COLLATE utf8_bin */;
create index easytax_tax_codes_product_idx on easytax_tax_codes(tax_zone, product_name, tax_code);
