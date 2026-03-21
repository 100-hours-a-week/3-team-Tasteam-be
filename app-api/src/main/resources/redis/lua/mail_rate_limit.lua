-- Mail send rate limiter (fixed window + progressive 24h block)
--
-- KEYS
-- 1: email_1m_key
-- 2: ip_1m_key
-- 3: user_1m_key (optional: empty string to skip)
-- 4: email_1d_key
-- 5: email_block_key
--
-- ARGV
-- 1: nowEpochSeconds (reserved for logging/debug)
-- 2: limit_email_1m
-- 3: limit_ip_1m
-- 4: limit_user_1m
-- 5: limit_email_1d
-- 6: ttl_1m
-- 7: ttl_1d
-- 8: ttl_block
--
-- Return: {allowed(1|0), reason, retry_after_seconds}
-- reason values:
--   ALLOWED
--   EMAIL_BLOCKED_24H
--   RATE_LIMIT_EMAIL_1M
--   RATE_LIMIT_IP_1M
--   RATE_LIMIT_USER_1M

local email1mKey = KEYS[1]
local ip1mKey = KEYS[2]
local user1mKey = KEYS[3]
local email1dKey = KEYS[4]
local blockKey = KEYS[5]

local limitEmail1m = tonumber(ARGV[2])
local limitIp1m = tonumber(ARGV[3])
local limitUser1m = tonumber(ARGV[4])
local limitEmail1d = tonumber(ARGV[5])
local ttl1m = tonumber(ARGV[6])
local ttl1d = tonumber(ARGV[7])
local ttlBlock = tonumber(ARGV[8])

local function has_key(key)
  return key ~= nil and key ~= ''
end

local function safe_ttl(key)
  if not has_key(key) then
    return 0
  end
  local ttl = tonumber(redis.call('TTL', key))
  if ttl == nil or ttl < 0 then
    return 0
  end
  return ttl
end

local function reject(reason, key)
  return {0, reason, safe_ttl(key)}
end

local function get_count(key)
  if not has_key(key) then
    return 0
  end
  local value = redis.call('GET', key)
  if value == false or value == nil then
    return 0
  end
  return tonumber(value) or 0
end

local function incr_with_ttl(key, ttl)
  if not has_key(key) then
    return 0
  end
  local value = tonumber(redis.call('INCR', key))
  if value == 1 then
    redis.call('EXPIRE', key, ttl)
  end
  return value
end

-- 1) 24h block first
if has_key(blockKey) and redis.call('EXISTS', blockKey) == 1 then
  return reject('EMAIL_BLOCKED_24H', blockKey)
end

-- 2) Minute-level pre-checks (email/ip/user)
if get_count(email1mKey) >= limitEmail1m then
  return reject('RATE_LIMIT_EMAIL_1M', email1mKey)
end

if get_count(ip1mKey) >= limitIp1m then
  return reject('RATE_LIMIT_IP_1M', ip1mKey)
end

if has_key(user1mKey) and get_count(user1mKey) >= limitUser1m then
  return reject('RATE_LIMIT_USER_1M', user1mKey)
end

-- 3) Increment all counters atomically
incr_with_ttl(email1mKey, ttl1m)
incr_with_ttl(ip1mKey, ttl1m)
incr_with_ttl(user1mKey, ttl1m)
local email1dCount = incr_with_ttl(email1dKey, ttl1d)

-- 4) Progressive block: allow up to 10/day, block from 11th request
if email1dCount > limitEmail1d then
  redis.call('SET', blockKey, '1', 'EX', ttlBlock)
  return {0, 'EMAIL_BLOCKED_24H', ttlBlock}
end

return {1, 'ALLOWED', 0}
