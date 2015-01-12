USE slick_tryouts;

INSERT INTO supplier(id, name, address) VALUES
  (101, 'Birds Inc.', '61 Bird Street, Bird Town'),
  (102, 'Soap Company', '537 Paper Street, Bradford');

INSERT INTO product(id, supplier_id, name) VALUES
  (201, 101, 'Defeathering'),
  (202, 101, 'Norwegian Blue'),
  (203, 102, 'All Natural');

INSERT INTO purchaser(id, name, address) VALUES
  (301, 'Michael Palin', NULL),
  (302, 'Hilton', 'New York');

INSERT INTO sale(id, purchaser_id, product_id, total) VALUES
  (401, 301, 202, 499.99),
  (402, 301, 201, 100.00),
  (403, 302, 203, 1000.00),
  (404, 302, 201, 2387.49);