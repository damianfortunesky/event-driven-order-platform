INSERT INTO inventory_stock(product_id, available_quantity, updated_at)
VALUES
  ('11111111-1111-1111-1111-111111111111', 10, now()),
  ('22222222-2222-2222-2222-222222222222', 3, now()),
  ('33333333-3333-3333-3333-333333333333', 0, now())
ON CONFLICT (product_id) DO NOTHING;
