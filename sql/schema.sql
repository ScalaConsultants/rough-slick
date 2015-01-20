-- DROP DATABASE IF EXISTS slick_tryouts;
-- CREATE DATABASE slick_tryouts;

-- use slick_tryouts;

CREATE TABLE supplier (
  id INT,
  name VARCHAR(255),
  address VARCHAR(255),

  PRIMARY KEY (id)
);

CREATE TABLE purchaser (
  id INT,
  name VARCHAR(255),
  address VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE product (
  id INT,
  supplier_id INT,
  name VARCHAR(255),

  PRIMARY KEY (id),
  FOREIGN KEY (supplier_id) REFERENCES supplier(id)
);

CREATE TABLE sale (
  id INT,
  purchaser_id INT,
  product_id INT,
  total DECIMAL,

  PRIMARY KEY (id),
  FOREIGN KEY (purchaser_id) REFERENCES purchaser(id),
  FOREIGN KEY (product_id) REFERENCES product(id)
);