-- Default category catalogue and global merchant rules (US-2.1, US-7.1).

INSERT INTO categories (id, user_id, system_key, name, color, kind) VALUES
    ('00000000-0000-4000-8000-000000000001', NULL, 'groceries',     'Groceries',      '#4CAF50', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000002', NULL, 'eating_out',    'Eating out',     '#FF9800', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000003', NULL, 'transport',     'Transport',      '#2196F3', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000004', NULL, 'housing',       'Housing',        '#795548', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000005', NULL, 'utilities',     'Utilities',      '#607D8B', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000006', NULL, 'subscriptions', 'Subscriptions',  '#9C27B0', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000007', NULL, 'shopping',      'Shopping',       '#E91E63', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000008', NULL, 'health',        'Health',         '#F44336', 'EXPENSE'),
    ('00000000-0000-4000-8000-000000000009', NULL, 'entertainment', 'Entertainment',  '#673AB7', 'EXPENSE'),
    ('00000000-0000-4000-8000-00000000000a', NULL, 'travel',        'Travel',         '#00BCD4', 'EXPENSE'),
    ('00000000-0000-4000-8000-00000000000b', NULL, 'education',     'Education',      '#3F51B5', 'EXPENSE'),
    ('00000000-0000-4000-8000-00000000000c', NULL, 'other',         'Other',          '#9E9E9E', 'EXPENSE'),
    ('00000000-0000-4000-8000-00000000000d', NULL, 'income',        'Income',         '#2E7D32', 'INCOME'),
    ('00000000-0000-4000-8000-00000000000e', NULL, 'transfers',     'Transfers',      '#78909C', 'TRANSFER');

INSERT INTO merchant_rules (id, user_id, pattern, category_id, created_at)
SELECT gen_random_uuid(), NULL, r.pattern, c.id, NOW()
FROM (VALUES
    ('LIDL', 'groceries'), ('ALDI', 'groceries'), ('CARREFOUR', 'groceries'), ('TESCO', 'groceries'),
    ('MAXIMA', 'groceries'), ('RIMI', 'groceries'), ('IKI', 'groceries'), ('BILLA', 'groceries'),
    ('MCDONALD', 'eating_out'), ('KFC', 'eating_out'), ('STARBUCKS', 'eating_out'), ('BURGER KING', 'eating_out'),
    ('WOLT', 'eating_out'), ('BOLT FOOD', 'eating_out'), ('UBER EATS', 'eating_out'),
    ('UBER', 'transport'), ('BOLT', 'transport'), ('CIRCLE K', 'transport'), ('SHELL', 'transport'),
    ('NESTE', 'transport'), ('RYANAIR', 'travel'), ('WIZZ AIR', 'travel'), ('AIRBNB', 'travel'),
    ('BOOKING.COM', 'travel'),
    ('NETFLIX', 'subscriptions'), ('SPOTIFY', 'subscriptions'), ('YOUTUBE PREMIUM', 'subscriptions'),
    ('APPLE.COM BILL', 'subscriptions'), ('GOOGLE STORAGE', 'subscriptions'), ('DISNEY PLUS', 'subscriptions'),
    ('AMAZON', 'shopping'), ('ZALANDO', 'shopping'), ('IKEA', 'shopping'), ('H&M', 'shopping'),
    ('APOTHEKA', 'health'), ('PHARMACY', 'health'), ('STEAM', 'entertainment'), ('CINEMA', 'entertainment'),
    ('SALARY', 'income'), ('TOP-UP', 'transfers'), ('TRANSFER TO', 'transfers'), ('TRANSFER FROM', 'transfers')
) AS r(pattern, system_key)
JOIN categories c ON c.system_key = r.system_key;
