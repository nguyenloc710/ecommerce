-- Trừ suất flash sale cho một đơn, tất cả hoặc không gì cả.
-- KEYS: mỗi item 2 khóa liên tiếp: suất còn, số khách đã dùng.
-- ARGV: mỗi item 5 giá trị liên tiếp: số lượng, giới hạn theo khách (0 = không), suất còn theo DB,
--       khách đã dùng theo DB, TTL (giây). Hai giá trị DB chỉ dùng khi khóa chưa có (lần đầu hoặc Valkey mất dữ liệu).
-- Trả 0 nếu thành công, i nếu item thứ i hết suất, -i nếu item thứ i vượt giới hạn theo khách.
local n = #KEYS / 2

for i = 1, n do
  local a = (i - 1) * 5
  redis.call('SET', KEYS[2 * i - 1], ARGV[a + 3], 'NX', 'EX', ARGV[a + 5])
  redis.call('SET', KEYS[2 * i], ARGV[a + 4], 'NX', 'EX', ARGV[a + 5])
end

for i = 1, n do
  local a = (i - 1) * 5
  local qty = tonumber(ARGV[a + 1])
  local limit = tonumber(ARGV[a + 2])
  if tonumber(redis.call('GET', KEYS[2 * i - 1])) < qty then
    return i
  end
  if limit > 0 and tonumber(redis.call('GET', KEYS[2 * i])) + qty > limit then
    return -i
  end
end

for i = 1, n do
  local qty = tonumber(ARGV[(i - 1) * 5 + 1])
  redis.call('DECRBY', KEYS[2 * i - 1], qty)
  redis.call('INCRBY', KEYS[2 * i], qty)
end
return 0
