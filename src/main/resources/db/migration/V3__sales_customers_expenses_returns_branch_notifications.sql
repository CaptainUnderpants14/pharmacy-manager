CREATE TABLE branches (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(100),
    is_main BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    customer_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(100),
    address VARCHAR(255),
    gender VARCHAR(20),
    date_of_birth DATE,
    medical_history TEXT,
    credit_balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE prescriptions (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    prescription_number VARCHAR(60) NOT NULL UNIQUE,
    customer_id UUID REFERENCES customers(id),
    doctor_name VARCHAR(255) NOT NULL,
    doctor_license_number VARCHAR(100),
    hospital_name VARCHAR(255),
    prescription_date DATE NOT NULL,
    notes TEXT,
    file_url VARCHAR(500),
    status VARCHAR(30) NOT NULL
);

CREATE TABLE sales (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    sale_number VARCHAR(60) NOT NULL UNIQUE,
    customer_id UUID REFERENCES customers(id),
    branch_id UUID REFERENCES branches(id),
    prescription_id UUID REFERENCES prescriptions(id),
    sale_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    discount NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(14,2) NOT NULL,
    paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    change_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    notes VARCHAR(500),
    created_by_id UUID REFERENCES users(id)
);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    sale_id UUID NOT NULL REFERENCES sales(id),
    medicine_id UUID NOT NULL REFERENCES medicines(id),
    medicine_batch_id UUID REFERENCES medicine_batches(id),
    batch_number VARCHAR(255),
    quantity NUMERIC(14,3) NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL,
    discount NUMERIC(14,2) NOT NULL DEFAULT 0,
    tax NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL
);

CREATE TABLE sale_returns (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    return_number VARCHAR(60) NOT NULL UNIQUE,
    sale_id UUID NOT NULL REFERENCES sales(id),
    customer_id UUID REFERENCES customers(id),
    return_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    tax NUMERIC(14,2) NOT NULL DEFAULT 0,
    refund_amount NUMERIC(14,2) NOT NULL,
    refund_method VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    created_by_id UUID REFERENCES users(id)
);

CREATE TABLE sale_return_items (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    sale_return_id UUID NOT NULL REFERENCES sale_returns(id),
    medicine_batch_id UUID REFERENCES medicine_batches(id),
    quantity NUMERIC(14,3) NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL,
    refund_amount NUMERIC(14,2) NOT NULL,
    restock BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE purchase_returns (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    return_number VARCHAR(60) NOT NULL UNIQUE,
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    purchase_order_id UUID REFERENCES purchase_orders(id),
    return_date DATE NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    refund_status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    created_by_id UUID REFERENCES users(id)
);

CREATE TABLE purchase_return_items (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    purchase_return_id UUID NOT NULL REFERENCES purchase_returns(id),
    medicine_batch_id UUID NOT NULL REFERENCES medicine_batches(id),
    quantity NUMERIC(14,3) NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL
);

CREATE TABLE customer_payments (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    payment_number VARCHAR(60) NOT NULL UNIQUE,
    customer_id UUID REFERENCES customers(id),
    sale_id UUID REFERENCES sales(id),
    amount NUMERIC(14,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(120),
    payment_date DATE NOT NULL,
    notes VARCHAR(500),
    created_by_id UUID REFERENCES users(id)
);

CREATE TABLE expense_categories (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expense_number VARCHAR(60) NOT NULL UNIQUE,
    category_id UUID NOT NULL REFERENCES expense_categories(id),
    amount NUMERIC(14,2) NOT NULL,
    expense_date DATE NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    reference_number VARCHAR(120),
    payee VARCHAR(255),
    description VARCHAR(500),
    branch_id UUID REFERENCES branches(id),
    created_by_id UUID REFERENCES users(id)
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    read_status BOOLEAN NOT NULL DEFAULT FALSE,
    recipient_user_id UUID REFERENCES users(id)
);

CREATE INDEX idx_branch_code ON branches(code);
CREATE INDEX idx_customer_code ON customers(customer_code);
CREATE INDEX idx_customer_phone ON customers(phone);
CREATE INDEX idx_sales_number ON sales(sale_number);
CREATE INDEX idx_sales_customer ON sales(customer_id);
CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE INDEX idx_expense_category ON expenses(category_id);
CREATE INDEX idx_expense_date ON expenses(expense_date);
CREATE INDEX idx_notification_user ON notifications(recipient_user_id);
