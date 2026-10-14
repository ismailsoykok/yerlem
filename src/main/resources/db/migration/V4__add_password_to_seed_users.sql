UPDATE users
SET password_hash = '$2a$10$EOTXeON1wUQ0JbUzk7BiIOLvwwonFpm6nQBGG2drQgRApml/moWXa'
WHERE phone IN (
                '5550000001',
                '5550000002',
                '5550000003',
                '5550000004',
                '5550000005'
    );