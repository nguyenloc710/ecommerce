-- Dữ liệu mẫu cho môi trường local (không nạp trên Kubernetes, xem spring.flyway.locations).
-- Idempotent: ON CONFLICT DO NOTHING theo id cố định.

INSERT INTO category (id, parent_id, name, slug, attribute_template, sort_order, status, created_at, created_by, updated_at, updated_by)
VALUES
    ('10000000-0000-0000-0000-000000000001', NULL, 'Thời trang', 'thoi-trang', '{}', 1, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000002', NULL, 'Phụ kiện', 'phu-kien', '{}', 2, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000003', NULL, 'Trang sức', 'trang-suc', '{}', 3, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000001', 'Áo', 'ao',
     '{"options":{"size":["S","M","L","XL"],"color":[]},"attributes":["Chất liệu","Form dáng"]}', 1, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000001', 'Váy & Quần', 'vay-quan',
     '{"options":{"size":["S","M","L"],"color":[]},"attributes":["Chất liệu","Form dáng"]}', 2, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000021', '10000000-0000-0000-0000-000000000002', 'Túi', 'tui',
     '{"options":{"color":[]},"attributes":["Chất liệu","Kích thước"]}', 1, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000022', '10000000-0000-0000-0000-000000000002', 'Thắt lưng', 'that-lung',
     '{"options":{"color":[],"size":["S","M","L"]},"attributes":["Chất liệu"]}', 2, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000031', '10000000-0000-0000-0000-000000000003', 'Nhẫn', 'nhan',
     '{"options":{"color":["Bạc","Mạ vàng","Vàng hồng"],"size":["5","6","7","8","9"]},"attributes":["Chất liệu","Đá chủ","Trọng lượng","Kiểm định"]}', 1, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000032', '10000000-0000-0000-0000-000000000003', 'Dây chuyền', 'day-chuyen',
     '{"options":{"color":["Bạc","Mạ vàng"],"length":["40 cm","45 cm"]},"attributes":["Chất liệu","Đá chủ","Trọng lượng"]}', 2, 'ACTIVE', now(), 'system', now(), 'system'),
    ('10000000-0000-0000-0000-000000000033', '10000000-0000-0000-0000-000000000003', 'Khuyên tai', 'khuyen-tai',
     '{"options":{"color":["Bạc","Mạ vàng"]},"attributes":["Chất liệu","Đá chủ"]}', 3, 'ACTIVE', now(), 'system', now(), 'system')
ON CONFLICT (id) DO NOTHING;

INSERT INTO brand (id, name, slug, created_at, created_by, updated_at, updated_by)
VALUES ('20000000-0000-0000-0000-000000000001', 'Nhà may', 'nha-may', now(), 'system', now(), 'system')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product (id, category_id, brand_id, slug, name, description, attributes, pricing_type, status, published_at, created_at, created_by, updated_at, updated_by)
VALUES
    ('30000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001',
     'ao-so-mi-linen-oversize', 'Áo sơ mi linen oversize', 'Linen 100%, form rộng, cổ đức. Giặt máy chế độ nhẹ.',
     '{"Chất liệu":"Linen 100%","Form dáng":"Oversize"}', 'FIXED', 'ACTIVE', now() - interval '3 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000001',
     'ao-thun-cotton-basic', 'Áo thun cotton basic', 'Cotton 2 chiều 220 gsm, cổ tròn.',
     '{"Chất liệu":"Cotton 100%","Form dáng":"Regular"}', 'FIXED', 'ACTIVE', now() - interval '60 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000001',
     'vay-midi-lua-co-vuong', 'Váy midi lụa cổ vuông', 'Lụa habutai, lót trong, khóa kéo sau.',
     '{"Chất liệu":"Lụa","Form dáng":"Midi"}', 'FIXED', 'ACTIVE', now() - interval '5 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000001',
     'quan-ong-rong-dang-suong', 'Quần ống rộng dáng suông', 'Vải tuyết mưa co giãn nhẹ, lưng cao.',
     '{"Chất liệu":"Tuyết mưa","Form dáng":"Ống rộng"}', 'FIXED', 'ACTIVE', now() - interval '45 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000001',
     'tui-deo-cheo-da-bo', 'Túi đeo chéo da bò', 'Da bò thật, khóa đồng, dây đeo điều chỉnh.',
     '{"Chất liệu":"Da bò","Kích thước":"22 × 15 × 7 cm"}', 'FIXED', 'ACTIVE', now() - interval '10 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000022', '20000000-0000-0000-0000-000000000001',
     'that-lung-da-ban-nho', 'Thắt lưng da bản nhỏ', 'Da bò bản 2 cm, khóa kim.',
     '{"Chất liệu":"Da bò"}', 'FIXED', 'ACTIVE', now() - interval '90 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000001',
     'nhan-bac-dinh-da-moissanite-5-ly', 'Nhẫn bạc đính đá moissanite 5 ly', 'Ổ 4 chấu, đá chủ moissanite 5 ly nước D VVS, cắt 57 giác. Đai 1,8 mm.',
     '{"Chất liệu":"Bạc 925 mạ rhodium","Đá chủ":"Moissanite 5 ly","Trọng lượng":"2,4 g","Kiểm định":"Có"}', 'FIXED', 'ACTIVE', now() - interval '2 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000001',
     'nhan-bac-tron-ban-2mm', 'Nhẫn bạc trơn bản 2 mm', 'Bạc 925 đánh bóng gương.',
     '{"Chất liệu":"Bạc 925","Trọng lượng":"1,6 g"}', 'FIXED', 'ACTIVE', now() - interval '120 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000001',
     'nhan-doi-bac-khac-ten', 'Nhẫn đôi bạc khắc tên', 'Cặp nhẫn bạc 925, khắc tên miễn phí.',
     '{"Chất liệu":"Bạc 925","Trọng lượng":"4,0 g"}', 'FIXED', 'ACTIVE', now() - interval '20 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-00000000000a', '10000000-0000-0000-0000-000000000032', '20000000-0000-0000-0000-000000000001',
     'day-chuyen-bac-mat-da-4-ly', 'Dây chuyền bạc mặt đá 4 ly', 'Dây bạc 925 mắt xích tròn, mặt đá CZ 4 ly.',
     '{"Chất liệu":"Bạc 925","Đá chủ":"CZ 4 ly","Trọng lượng":"3,1 g"}', 'FIXED', 'ACTIVE', now() - interval '8 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-00000000000b', '10000000-0000-0000-0000-000000000033', '20000000-0000-0000-0000-000000000001',
     'khuyen-tai-ngoc-trai-nuoc-ngot', 'Khuyên tai ngọc trai nước ngọt', 'Ngọc trai nước ngọt 7–8 mm, chốt bạc.',
     '{"Chất liệu":"Bạc 925","Đá chủ":"Ngọc trai nước ngọt"}', 'FIXED', 'ACTIVE', now() - interval '15 days', now(), 'system', now(), 'system'),
    ('30000000-0000-0000-0000-00000000000c', '10000000-0000-0000-0000-000000000031', '20000000-0000-0000-0000-000000000001',
     'nhan-bac-dinh-ngoc-luc-bao', 'Nhẫn bạc đính ngọc lục bảo', 'Ngọc lục bảo tự nhiên 3 × 5 mm.',
     '{"Chất liệu":"Bạc 925","Đá chủ":"Ngọc lục bảo"}', 'FIXED', 'ACTIVE', now() - interval '200 days', now(), 'system', now(), 'system')
ON CONFLICT (id) DO NOTHING;

INSERT INTO product_variant (id, product_id, sku_code, option_values, price, compare_at_price, weight_gram, status, sort_order, created_at, created_by, updated_at, updated_by)
VALUES
    -- Áo sơ mi linen: 2 màu × 3 size
    ('40000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000001', 'SMLN-WH-S', '{"color":"Trắng","size":"S"}', 590000, NULL, 250, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000102', '30000000-0000-0000-0000-000000000001', 'SMLN-WH-M', '{"color":"Trắng","size":"M"}', 590000, NULL, 260, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000103', '30000000-0000-0000-0000-000000000001', 'SMLN-WH-L', '{"color":"Trắng","size":"L"}', 590000, NULL, 270, 'ACTIVE', 3, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000104', '30000000-0000-0000-0000-000000000001', 'SMLN-BE-S', '{"color":"Be","size":"S"}', 590000, NULL, 250, 'ACTIVE', 4, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000105', '30000000-0000-0000-0000-000000000001', 'SMLN-BE-M', '{"color":"Be","size":"M"}', 590000, NULL, 260, 'ACTIVE', 5, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000106', '30000000-0000-0000-0000-000000000001', 'SMLN-BE-L', '{"color":"Be","size":"L"}', 590000, NULL, 270, 'ACTIVE', 6, now(), 'system', now(), 'system'),
    -- Áo thun basic: giảm giá
    ('40000000-0000-0000-0000-000000000201', '30000000-0000-0000-0000-000000000002', 'TSBS-BK-M', '{"color":"Đen","size":"M"}', 174000, 249000, 200, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000202', '30000000-0000-0000-0000-000000000002', 'TSBS-BK-L', '{"color":"Đen","size":"L"}', 174000, 249000, 210, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000203', '30000000-0000-0000-0000-000000000002', 'TSBS-WH-M', '{"color":"Trắng","size":"M"}', 174000, 249000, 200, 'ACTIVE', 3, now(), 'system', now(), 'system'),
    -- Váy midi lụa
    ('40000000-0000-0000-0000-000000000301', '30000000-0000-0000-0000-000000000003', 'VMDL-GR-S', '{"color":"Xanh rêu","size":"S"}', 1290000, NULL, 300, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000302', '30000000-0000-0000-0000-000000000003', 'VMDL-GR-M', '{"color":"Xanh rêu","size":"M"}', 1290000, NULL, 310, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000303', '30000000-0000-0000-0000-000000000003', 'VMDL-PK-M', '{"color":"Hồng đất","size":"M"}', 1290000, NULL, 310, 'ACTIVE', 3, now(), 'system', now(), 'system'),
    -- Quần ống rộng: giảm giá
    ('40000000-0000-0000-0000-000000000401', '30000000-0000-0000-0000-000000000004', 'QORS-BK-M', '{"color":"Đen","size":"M"}', 552000, 650000, 350, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000402', '30000000-0000-0000-0000-000000000004', 'QORS-BK-L', '{"color":"Đen","size":"L"}', 552000, 650000, 360, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    -- Túi da
    ('40000000-0000-0000-0000-000000000501', '30000000-0000-0000-0000-000000000005', 'TDCD-BR', '{"color":"Nâu"}', 1650000, NULL, 600, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000502', '30000000-0000-0000-0000-000000000005', 'TDCD-BK', '{"color":"Đen"}', 1650000, NULL, 600, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    -- Thắt lưng: giảm giá
    ('40000000-0000-0000-0000-000000000601', '30000000-0000-0000-0000-000000000006', 'TLDB-BK-M', '{"color":"Đen","size":"M"}', 299000, 399000, 150, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    -- Nhẫn moissanite: 3 màu kim loại × size 5-9 (rút gọn)
    ('40000000-0000-0000-0000-000000000701', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-SL-5', '{"color":"Bạc","size":"5"}', 890000, 1090000, 2, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000702', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-SL-6', '{"color":"Bạc","size":"6"}', 890000, 1090000, 2, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000703', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-SL-7', '{"color":"Bạc","size":"7"}', 890000, 1090000, 2, 'ACTIVE', 3, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000704', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-SL-8', '{"color":"Bạc","size":"8"}', 890000, 1090000, 2, 'ACTIVE', 4, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000705', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-SL-9', '{"color":"Bạc","size":"9"}', 890000, 1090000, 2, 'ACTIVE', 5, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000706', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-GD-6', '{"color":"Mạ vàng","size":"6"}', 890000, 1090000, 2, 'ACTIVE', 6, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000707', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-GD-7', '{"color":"Mạ vàng","size":"7"}', 890000, 1090000, 2, 'ACTIVE', 7, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000708', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-RG-6', '{"color":"Vàng hồng","size":"6"}', 890000, 1090000, 2, 'ACTIVE', 8, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000709', '30000000-0000-0000-0000-000000000007', 'RNG-MS5-RG-7', '{"color":"Vàng hồng","size":"7"}', 890000, 1090000, 2, 'ACTIVE', 9, now(), 'system', now(), 'system'),
    -- Nhẫn trơn
    ('40000000-0000-0000-0000-000000000801', '30000000-0000-0000-0000-000000000008', 'RNG-PL2-SL-6', '{"color":"Bạc","size":"6"}', 350000, NULL, 2, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000802', '30000000-0000-0000-0000-000000000008', 'RNG-PL2-SL-7', '{"color":"Bạc","size":"7"}', 350000, NULL, 2, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000803', '30000000-0000-0000-0000-000000000008', 'RNG-PL2-GD-7', '{"color":"Mạ vàng","size":"7"}', 350000, NULL, 2, 'ACTIVE', 3, now(), 'system', now(), 'system'),
    -- Nhẫn đôi: giảm giá
    ('40000000-0000-0000-0000-000000000901', '30000000-0000-0000-0000-000000000009', 'RNG-CPL-SL', '{"color":"Bạc"}', 792000, 990000, 4, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    -- Dây chuyền
    ('40000000-0000-0000-0000-000000000a01', '30000000-0000-0000-0000-00000000000a', 'NCK-CZ4-SL-45', '{"color":"Bạc","length":"45 cm"}', 750000, NULL, 3, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    ('40000000-0000-0000-0000-000000000a02', '30000000-0000-0000-0000-00000000000a', 'NCK-CZ4-GD-45', '{"color":"Mạ vàng","length":"45 cm"}', 790000, NULL, 3, 'ACTIVE', 2, now(), 'system', now(), 'system'),
    -- Khuyên tai: giảm giá
    ('40000000-0000-0000-0000-000000000b01', '30000000-0000-0000-0000-00000000000b', 'EAR-PRL-SL', '{"color":"Bạc"}', 392000, 490000, 2, 'ACTIVE', 1, now(), 'system', now(), 'system'),
    -- Nhẫn ngọc lục bảo
    ('40000000-0000-0000-0000-000000000c01', '30000000-0000-0000-0000-00000000000c', 'RNG-EMR-SL-6', '{"color":"Bạc","size":"6"}', 1250000, NULL, 3, 'ACTIVE', 1, now(), 'system', now(), 'system')
ON CONFLICT (id) DO NOTHING;

-- Ảnh mẫu (file tĩnh trong storefront/public/img/seed, nguồn Pexels; xem CREDITS.md ở đó). Thay bằng ảnh thật ở module media.
INSERT INTO product_image (id, product_id, variant_id, url, alt, sort_order, created_at, created_by, updated_at, updated_by)
VALUES
    ('50000000-0000-0000-0000-000000000101', '30000000-0000-0000-0000-000000000001', NULL, '/img/seed/ao-so-mi-linen-oversize-1.jpg', 'Áo sơ mi linen oversize', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000102', '30000000-0000-0000-0000-000000000001', NULL, '/img/seed/ao-so-mi-linen-oversize-2.jpg', 'Áo sơ mi linen oversize - mặt sau', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000201', '30000000-0000-0000-0000-000000000002', NULL, '/img/seed/ao-thun-cotton-basic-1.jpg', 'Áo thun cotton basic', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000202', '30000000-0000-0000-0000-000000000002', NULL, '/img/seed/ao-thun-cotton-basic-2.jpg', 'Áo thun cotton basic - chi tiết', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000301', '30000000-0000-0000-0000-000000000003', NULL, '/img/seed/vay-midi-lua-co-vuong-1.jpg', 'Váy midi lụa cổ vuông', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000302', '30000000-0000-0000-0000-000000000003', NULL, '/img/seed/vay-midi-lua-co-vuong-2.jpg', 'Váy midi lụa cổ vuông - toàn thân', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000401', '30000000-0000-0000-0000-000000000004', NULL, '/img/seed/quan-ong-rong-dang-suong-1.jpg', 'Quần ống rộng dáng suông', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000402', '30000000-0000-0000-0000-000000000004', NULL, '/img/seed/quan-ong-rong-dang-suong-2.jpg', 'Quần ống rộng dáng suông - phối đồ', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000501', '30000000-0000-0000-0000-000000000005', NULL, '/img/seed/tui-deo-cheo-da-bo-1.jpg', 'Túi đeo chéo da bò', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000601', '30000000-0000-0000-0000-000000000006', NULL, '/img/seed/that-lung-da-ban-nho-1.jpg', 'Thắt lưng da bản nhỏ', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000701', '30000000-0000-0000-0000-000000000007', NULL, '/img/seed/nhan-bac-dinh-da-moissanite-5-ly-1.jpg', 'Nhẫn bạc đính đá moissanite 5 ly', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000702', '30000000-0000-0000-0000-000000000007', NULL, '/img/seed/nhan-bac-dinh-da-moissanite-5-ly-2.jpg', 'Nhẫn bạc đính đá moissanite - cận đá', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000801', '30000000-0000-0000-0000-000000000008', NULL, '/img/seed/nhan-bac-tron-ban-2mm-1.jpg', 'Nhẫn bạc trơn bản 2 mm', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000802', '30000000-0000-0000-0000-000000000008', NULL, '/img/seed/nhan-bac-tron-ban-2mm-2.jpg', 'Nhẫn bạc trơn - cặp', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000901', '30000000-0000-0000-0000-000000000009', NULL, '/img/seed/nhan-doi-bac-khac-ten-1.jpg', 'Nhẫn đôi bạc khắc tên', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000902', '30000000-0000-0000-0000-000000000009', NULL, '/img/seed/nhan-doi-bac-khac-ten-2.jpg', 'Nhẫn đôi bạc khắc tên - trên tay', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000a01', '30000000-0000-0000-0000-00000000000a', NULL, '/img/seed/day-chuyen-bac-mat-da-4-ly-1.jpg', 'Dây chuyền bạc mặt đá 4 ly', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000a02', '30000000-0000-0000-0000-00000000000a', NULL, '/img/seed/day-chuyen-bac-mat-da-4-ly-2.jpg', 'Dây chuyền bạc mặt đá - cận mặt', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000b01', '30000000-0000-0000-0000-00000000000b', NULL, '/img/seed/khuyen-tai-ngoc-trai-nuoc-ngot-1.jpg', 'Khuyên tai ngọc trai nước ngọt', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000b02', '30000000-0000-0000-0000-00000000000b', NULL, '/img/seed/khuyen-tai-ngoc-trai-nuoc-ngot-2.jpg', 'Khuyên tai ngọc trai - chi tiết', 1, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000c01', '30000000-0000-0000-0000-00000000000c', NULL, '/img/seed/nhan-bac-dinh-ngoc-luc-bao-1.jpg', 'Nhẫn bạc đính ngọc lục bảo', 0, now(), 'system', now(), 'system'),
    ('50000000-0000-0000-0000-000000000c02', '30000000-0000-0000-0000-00000000000c', NULL, '/img/seed/nhan-bac-dinh-ngoc-luc-bao-2.jpg', 'Nhẫn bạc đính ngọc lục bảo - mặt đá', 1, now(), 'system', now(), 'system')
ON CONFLICT (id) DO NOTHING;
