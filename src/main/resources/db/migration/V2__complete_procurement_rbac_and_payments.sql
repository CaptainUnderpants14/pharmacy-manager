alter table purchase_order_items add column received_quantity numeric(14,3) not null default 0;

create table purchase_invoices (
  id uuid primary key, version bigint not null, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null,
  invoice_number varchar(100) not null unique, invoice_date date not null, supplier_id uuid not null references suppliers(id),
  purchase_order_id uuid references purchase_orders(id), subtotal numeric(14,2) not null, discount numeric(14,2) not null,
  tax numeric(14,2) not null, total numeric(14,2) not null, payment_status varchar(30) not null
);
create table supplier_payments (
  id uuid primary key, version bigint not null, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null,
  supplier_id uuid not null references suppliers(id), purchase_invoice_id uuid not null references purchase_invoices(id),
  amount numeric(14,2) not null, payment_method varchar(30) not null, reference_number varchar(120),
  payment_date date not null, notes varchar(1000), created_by_id uuid references users(id)
);
create table demo_payment_transactions (
  id uuid primary key, version bigint not null, created_at timestamp(6) with time zone not null, updated_at timestamp(6) with time zone not null,
  reference_type varchar(80) not null, reference_id uuid, amount numeric(14,2) not null, payment_method varchar(30) not null,
  status varchar(30) not null, terminal_reference varchar(120) not null unique, notes varchar(1000), created_by_id uuid references users(id)
);
create index idx_invoice_supplier on purchase_invoices(supplier_id);
create index idx_invoice_payment_status on purchase_invoices(payment_status);
create index idx_supplier_payment_invoice on supplier_payments(purchase_invoice_id);
create index idx_demo_payment_reference on demo_payment_transactions(reference_type, reference_id);
