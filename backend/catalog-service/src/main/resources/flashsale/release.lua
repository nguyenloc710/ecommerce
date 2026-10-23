-- Trả suất flash sale của một đơn.
-- KEYS: mỗi item 2 khóa liên tiếp: suất còn, số khách đã dùng. ARGV: số lượng của từng item.
-- Khóa đã hết hạn/mất thì bỏ qua: lần khởi tạo sau đọc lại từ DB (hold đã chuyển RELEASED trước khi gọi script này).
for i = 1, #ARGV do
  local qty = tonumber(ARGV[i])
  if redis.call('EXISTS', KEYS[2 * i - 1]) == 1 then
    redis.call('INCRBY', KEYS[2 * i - 1], qty)
  end
  if redis.call('EXISTS', KEYS[2 * i]) == 1 and redis.call('DECRBY', KEYS[2 * i], qty) < 0 then
    redis.call('SET', KEYS[2 * i], 0, 'KEEPTTL')
  end
end
return 0
