-- FUB demo-video data for lion4464@naver.com.
-- Idempotent: safe to run again before recording a demo.

BEGIN;

WITH demo_user AS (
  SELECT user_sq
  FROM tb_fy_user
  WHERE email = 'lion4464@naver.com'
), wiki_seed(category, entry_key, summary, content_json) AS (
  VALUES
    ('PROFILE', 'HOUSEHOLD_SIZE', '가구 구성과 소비 주기',
      '{"householdSize":"3인 가구"}'::jsonb),
    ('PET', 'PETS', '반려생활에 필요한 제품',
      '{"pets":["중형견 1마리","피부 민감성 사료 선호"]}'::jsonb),
    ('SHOPPING', 'SHOPPING_PRIORITIES', '구매 결정을 내리는 기준',
      '{"priorities":["최저가 우선","무료배송 선호","일정 2일 전 도착"]}'::jsonb),
    ('SHOPPING', 'PREFERRED_CATEGORIES', '자주 확인하는 상품군',
      '{"categories":["캠핑용품","반려동물용품","커피","생활용품"]}'::jsonb),
    ('FOOD', 'FAVORITE_FOODS', '즐겨 찾는 음식과 음료',
      '{"foods":["한식","홈카페","건강식"]}'::jsonb),
    ('LIFESTYLE', 'COOKING_FREQUENCY', '평소 식생활 패턴',
      '{"frequency":"주 3~4회 직접 요리"}'::jsonb),
    ('HOBBY', 'HOBBIES', '일정 추천에 반영할 취미',
      '{"hobbies":["캠핑","국내여행","홈카페"]}'::jsonb),
    ('PREFERENCE', 'ONBOARDING_PROMPT', '미리 알려주면 좋은 구매 습관',
      '{"prompt":"2만원 이상 상품은 가격을 비교하고, 캠핑 일정 2일 전까지 준비물을 주문해 주세요."}'::jsonb)
)
INSERT INTO tb_fy_wiki_entry (
  user_sq, category, entry_key, summary, content_json, source_type,
  confidence, status, sensitivity_level, version, create_date, modified_date
)
SELECT
  demo_user.user_sq, wiki_seed.category, wiki_seed.entry_key, wiki_seed.summary,
  wiki_seed.content_json, 'DEMO', 1.0000, 'ACTIVE', 'NORMAL', 1,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_user
CROSS JOIN wiki_seed
ON CONFLICT (user_sq, category, entry_key) DO UPDATE SET
  summary = EXCLUDED.summary,
  content_json = EXCLUDED.content_json,
  source_type = EXCLUDED.source_type,
  confidence = EXCLUDED.confidence,
  status = EXCLUDED.status,
  sensitivity_level = EXCLUDED.sensitivity_level,
  modified_date = CURRENT_TIMESTAMP;

INSERT INTO tb_fy_wiki_entry_history (
  wiki_entry_sq, user_sq, version, snapshot_json, changed_by,
  change_reason, create_date, modified_date
)
SELECT
  entry.wiki_entry_sq,
  entry.user_sq,
  entry.version,
  jsonb_build_object(
    'category', entry.category,
    'entryKey', entry.entry_key,
    'summary', entry.summary,
    'content', entry.content_json,
    'sourceType', entry.source_type,
    'confidence', entry.confidence,
    'status', entry.status,
    'sensitivityLevel', entry.sensitivity_level,
    'validUntil', entry.valid_until
  ),
  'DEMO',
  'DEMO_VIDEO_SEED',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM tb_fy_wiki_entry entry
JOIN tb_fy_user demo_user ON demo_user.user_sq = entry.user_sq
WHERE demo_user.email = 'lion4464@naver.com'
  AND entry.entry_key IN (
    'HOUSEHOLD_SIZE', 'PETS', 'SHOPPING_PRIORITIES', 'PREFERRED_CATEGORIES',
    'FAVORITE_FOODS', 'COOKING_FREQUENCY', 'HOBBIES', 'ONBOARDING_PROMPT'
  )
ON CONFLICT (wiki_entry_sq, version) DO UPDATE SET
  snapshot_json = EXCLUDED.snapshot_json,
  changed_by = EXCLUDED.changed_by,
  change_reason = EXCLUDED.change_reason,
  modified_date = CURRENT_TIMESTAMP;

-- Four samples produce a visible falling-price forecast around 2026-07-23.
DELETE FROM tb_fy_product_price_history
WHERE offer_sq = (
  SELECT offer_sq
  FROM tb_fy_product_offer
  WHERE provider = 'NAVER' AND external_product_id = '11124150101'
);

INSERT INTO tb_fy_product_price_history (
  offer_sq, price, shipping_fee, total_price, collected_at, create_date, modified_date
)
SELECT offer.offer_sq, sample.price, 0, sample.price, sample.collected_at,
       sample.collected_at, sample.collected_at
FROM tb_fy_product_offer offer
CROSS JOIN (
  VALUES
    (2500::bigint, TIMESTAMP '2026-07-10 09:00:00'),
    (400::bigint, TIMESTAMP '2026-07-14 09:00:00'),
    (1500::bigint, TIMESTAMP '2026-07-18 09:00:00'),
    (450::bigint, TIMESTAMP '2026-07-22 03:00:00')
) AS sample(price, collected_at)
WHERE offer.provider = 'NAVER'
  AND offer.external_product_id = '11124150101';

COMMIT;
