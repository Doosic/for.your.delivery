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
      '{"householdSize":"3인 가구","members":["성인 2명","청소년 1명"]}'::jsonb),
    ('PET', 'PETS', '반려생활에 필요한 제품',
      '{"pets":["중형견 1마리","피부 민감성 사료 선호"],"petName":"보리"}'::jsonb),
    ('PET', 'PET_REPURCHASE_CYCLE', '반려용품 재구매 주기',
      '{"cycles":["사료 28일","덴탈 간식 14일","배변용품 21일"]}'::jsonb),
    ('SHOPPING', 'SHOPPING_PRIORITIES', '구매 결정을 내리는 기준',
      '{"priorities":["최저가 우선","무료배송 선호","일정 2일 전 도착"]}'::jsonb),
    ('SHOPPING', 'PREFERRED_CATEGORIES', '자주 확인하는 상품군',
      '{"categories":["캠핑용품","반려동물용품","커피","생활용품"]}'::jsonb),
    ('SHOPPING', 'BUDGET_RULES', '가격대별 구매 기준',
      '{"rules":["2만원 이하는 역대가면 바로 구매","5만원 이상은 가격 추세 확인","배송비 포함 최저가 비교"]}'::jsonb),
    ('SHOPPING', 'DELIVERY_PREFERENCE', '선호 배송 방식',
      '{"preferences":["평일 저녁 수령","주말 배송 지양","일정 이틀 전 도착"]}'::jsonb),
    ('FOOD', 'FAVORITE_FOODS', '즐겨 찾는 음식과 음료',
      '{"foods":["한식","홈카페","건강식"],"coffee":"산미 적은 원두"}'::jsonb),
    ('LIFESTYLE', 'COOKING_FREQUENCY', '평소 식생활 패턴',
      '{"frequency":"주 3~4회 직접 요리","shoppingDay":"목요일"}'::jsonb),
    ('HOBBY', 'HOBBIES', '일정 추천에 반영할 취미',
      '{"hobbies":["캠핑","국내여행","홈카페"]}'::jsonb),
    ('LIFESTYLE', 'UPCOMING_NEEDS', '가까운 일정과 준비 방향',
      '{"upcoming":["주말 캠핑","제주 가족여행","홈카페 모임"],"priority":"캠핑 준비물 우선"}'::jsonb),
    ('PREFERENCE', 'ONBOARDING_PROMPT', '미리 알려주면 좋은 구매 습관',
      '{"prompt":"2만원 이상 상품은 가격을 비교하고, 캠핑 일정 2일 전까지 준비물을 주문해 주세요. 반려견 사료는 떨어지기 일주일 전에 알려주세요."}'::jsonb)
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
    'HOUSEHOLD_SIZE', 'PETS', 'PET_REPURCHASE_CYCLE', 'SHOPPING_PRIORITIES',
    'PREFERRED_CATEGORIES', 'BUDGET_RULES', 'DELIVERY_PREFERENCE',
    'FAVORITE_FOODS', 'COOKING_FREQUENCY', 'HOBBIES', 'UPCOMING_NEEDS',
    'ONBOARDING_PROMPT'
  )
ON CONFLICT (wiki_entry_sq, version) DO UPDATE SET
  snapshot_json = EXCLUDED.snapshot_json,
  changed_by = EXCLUDED.changed_by,
  change_reason = EXCLUDED.change_reason,
  modified_date = CURRENT_TIMESTAMP;

WITH product_seed(
  product_key, name, normalized_name, brand, maker, category_path, image_url
) AS (
  VALUES
    ('NAVER:FUB-DEMO-DOG-FOOD', '로얄캐닌 미니 인도어 시니어 3kg', '로얄캐닌 미니 인도어 시니어 3kg', '로얄캐닌', '로얄캐닌', '생활/건강 > 반려동물 > 강아지 사료 > 건식사료', 'https://shopping-phinf.pstatic.net/main_8802759/88027595160.10.jpg'),
    ('NAVER:FUB-DEMO-DOG-TREAT', '펫세븐 오메가3 소프트 츄 48개', '펫세븐 오메가3 소프트 츄 48개', '펫세븐', '펫세븐', '생활/건강 > 반려동물 > 강아지 간식 > 트릿/스틱', 'https://shopping-phinf.pstatic.net/main_6020353/60203534890.20260527105827.jpg'),
    ('NAVER:FUB-DEMO-DETERGENT', '초고농축 캡슐세제 리퀴드 200개입', '초고농축 캡슐세제 리퀴드 200개입', '리퀴드', '리퀴드', '생활/건강 > 생활용품 > 세제/세정제 > 세탁세제', 'https://shopping-phinf.pstatic.net/main_5964514/59645142669.20260413145201.jpg'),
    ('NAVER:FUB-DEMO-TISSUE', '친환경 대나무 화장지 3겹 30m 30롤', '친환경 대나무 화장지 3겹 30m 30롤', '담이연', '담이연', '생활/건강 > 생활용품 > 화장지 > 롤화장지', 'https://shopping-phinf.pstatic.net/main_5921571/59215713066.20260311113833.jpg'),
    ('NAVER:FUB-DEMO-COFFEE', '레쓰비 마일드 커피 200ml 30개', '레쓰비 마일드 커피 200ml 30개', '레쓰비', '롯데칠성음료', '식품 > 음료 > 커피 > 커피음료', 'https://shopping-phinf.pstatic.net/main_5287170/52871706649.20250207143539.jpg'),
    ('NAVER:FUB-DEMO-ICEBOX', '올리빙 도트 아이스박스 21L', '올리빙 도트 아이스박스 21l', '올리빙', '올리빙', '스포츠/레저 > 캠핑 > 아이스박스', 'https://shopping-phinf.pstatic.net/main_1435274/14352741831.20260701163036.jpg'),
    ('NAVER:FUB-DEMO-GRILL', '밥캠핑 엑스그릴 캠핑 화로대', '밥캠핑 엑스그릴 캠핑 화로대', '밥캠핑', '밥디자인', '스포츠/레저 > 캠핑 > 취사용품 > 바비큐그릴/화로대', 'https://shopping-phinf.pstatic.net/main_5739758/57397589981.20251027175552.jpg'),
    ('ELEVENST:FUB-DEMO-MOSQUITO', '원터치 캠핑 모기장 텐트', '원터치 캠핑 모기장 텐트', 'DM', 'DM', '스포츠/레저 > 캠핑 > 텐트용품 > 모기장', 'https://shopping-phinf.pstatic.net/main_8443168/84431685417.19.jpg'),
    ('ELEVENST:FUB-DEMO-TRAVEL', '여행용 휴대 빨래줄 세트', '여행용 휴대 빨래줄 세트', '아띠꼴로', '아띠꼴로', '생활/건강 > 여행용품 > 여행소품', 'https://shopping-phinf.pstatic.net/main_8273237/82732373077.9.jpg'),
    ('NAVER:FUB-DEMO-DEODORIZER', '위프 실내 고체 탈취제 300g 4개', '위프 실내 고체 탈취제 300g 4개', '위프', '위프', '생활/건강 > 생활용품 > 제습/방향/탈취 > 실내탈취제', 'https://shopping-phinf.pstatic.net/main_5930609/59306092109.20260618162805.jpg')
)
INSERT INTO tb_fy_product (
  product_key, name, normalized_name, brand, maker, category_path, image_url,
  status, create_date, modified_date
)
SELECT
  product_key, name, normalized_name, brand, maker, category_path, image_url,
  'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM product_seed
ON CONFLICT (product_key) DO UPDATE SET
  name = EXCLUDED.name,
  normalized_name = EXCLUDED.normalized_name,
  brand = EXCLUDED.brand,
  maker = EXCLUDED.maker,
  category_path = EXCLUDED.category_path,
  image_url = EXCLUDED.image_url,
  status = EXCLUDED.status,
  modified_date = CURRENT_TIMESTAMP;

WITH offer_seed(
  product_key, provider, external_product_id, mall_name, product_url,
  price, shipping_fee, total_price, search_rank
) AS (
  VALUES
    ('NAVER:FUB-DEMO-DOG-FOOD', 'NAVER', 'FUB-DEMO-DOG-FOOD', '네이버', 'https://search.shopping.naver.com/catalog/88027595160', 37600::bigint, 0::bigint, 37600::bigint, 1),
    ('NAVER:FUB-DEMO-DOG-TREAT', 'NAVER', 'FUB-DEMO-DOG-TREAT', '네이버', 'https://search.shopping.naver.com/catalog/60203534890', 22200::bigint, 0::bigint, 22200::bigint, 2),
    ('NAVER:FUB-DEMO-DETERGENT', 'NAVER', 'FUB-DEMO-DETERGENT', '네이버', 'https://search.shopping.naver.com/catalog/59645142669', 11000::bigint, 0::bigint, 11000::bigint, 3),
    ('NAVER:FUB-DEMO-TISSUE', 'NAVER', 'FUB-DEMO-TISSUE', '네이버', 'https://search.shopping.naver.com/catalog/59215713066', 17780::bigint, 0::bigint, 17780::bigint, 4),
    ('NAVER:FUB-DEMO-COFFEE', 'NAVER', 'FUB-DEMO-COFFEE', '네이버', 'https://search.shopping.naver.com/catalog/52871706649', 4770::bigint, 0::bigint, 4770::bigint, 5),
    ('NAVER:FUB-DEMO-ICEBOX', 'NAVER', 'FUB-DEMO-ICEBOX', '네이버', 'https://search.shopping.naver.com/catalog/14352741831', 27900::bigint, 0::bigint, 27900::bigint, 6),
    ('NAVER:FUB-DEMO-GRILL', 'NAVER', 'FUB-DEMO-GRILL', '네이버', 'https://search.shopping.naver.com/catalog/57397589981', 49800::bigint, 0::bigint, 49800::bigint, 7),
    ('ELEVENST:FUB-DEMO-MOSQUITO', 'ELEVENST', 'FUB-DEMO-MOSQUITO', '11번가', 'http://www.11st.co.kr/product/SellerProductDetail.tmall?method=getSellerProductDetail&prdNo=9479054418', 12600::bigint, 0::bigint, 12600::bigint, 8),
    ('ELEVENST:FUB-DEMO-TRAVEL', 'ELEVENST', 'FUB-DEMO-TRAVEL', '11번가', 'http://www.11st.co.kr/product/SellerProductDetail.tmall?method=getSellerProductDetail&prdNo=9067497349', 13500::bigint, 0::bigint, 13500::bigint, 9),
    ('NAVER:FUB-DEMO-DEODORIZER', 'NAVER', 'FUB-DEMO-DEODORIZER', '네이버', 'https://search.shopping.naver.com/catalog/59306092109', 54560::bigint, 0::bigint, 54560::bigint, 10)
)
INSERT INTO tb_fy_product_offer (
  product_sq, provider, external_product_id, mall_name, product_url,
  price, shipping_fee, total_price, search_rank, status, last_collected_at,
  create_date, modified_date
)
SELECT
  product.product_sq, offer_seed.provider, offer_seed.external_product_id,
  offer_seed.mall_name, offer_seed.product_url, offer_seed.price,
  offer_seed.shipping_fee, offer_seed.total_price, offer_seed.search_rank,
  'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM offer_seed
JOIN tb_fy_product product ON product.product_key = offer_seed.product_key
ON CONFLICT (provider, external_product_id) DO UPDATE SET
  product_sq = EXCLUDED.product_sq,
  mall_name = EXCLUDED.mall_name,
  product_url = EXCLUDED.product_url,
  price = EXCLUDED.price,
  shipping_fee = EXCLUDED.shipping_fee,
  total_price = EXCLUDED.total_price,
  search_rank = EXCLUDED.search_rank,
  status = EXCLUDED.status,
  last_collected_at = EXCLUDED.last_collected_at,
  modified_date = CURRENT_TIMESTAMP;

DELETE FROM tb_fy_product_price_history history
USING tb_fy_product_offer offer
WHERE history.offer_sq = offer.offer_sq
  AND offer.external_product_id LIKE 'FUB-DEMO-%';

WITH price_seed(provider, external_product_id, price, days_ago) AS (
  VALUES
    ('NAVER', 'FUB-DEMO-DOG-FOOD', 42000::bigint, 28), ('NAVER', 'FUB-DEMO-DOG-FOOD', 40500::bigint, 21), ('NAVER', 'FUB-DEMO-DOG-FOOD', 38900::bigint, 14), ('NAVER', 'FUB-DEMO-DOG-FOOD', 38200::bigint, 7), ('NAVER', 'FUB-DEMO-DOG-FOOD', 37600::bigint, 0),
    ('NAVER', 'FUB-DEMO-DOG-TREAT', 25000::bigint, 28), ('NAVER', 'FUB-DEMO-DOG-TREAT', 24400::bigint, 21), ('NAVER', 'FUB-DEMO-DOG-TREAT', 23200::bigint, 14), ('NAVER', 'FUB-DEMO-DOG-TREAT', 22900::bigint, 7), ('NAVER', 'FUB-DEMO-DOG-TREAT', 22200::bigint, 0),
    ('NAVER', 'FUB-DEMO-DETERGENT', 16900::bigint, 28), ('NAVER', 'FUB-DEMO-DETERGENT', 14900::bigint, 21), ('NAVER', 'FUB-DEMO-DETERGENT', 13900::bigint, 14), ('NAVER', 'FUB-DEMO-DETERGENT', 12900::bigint, 7), ('NAVER', 'FUB-DEMO-DETERGENT', 11000::bigint, 0),
    ('NAVER', 'FUB-DEMO-TISSUE', 18900::bigint, 28), ('NAVER', 'FUB-DEMO-TISSUE', 18200::bigint, 21), ('NAVER', 'FUB-DEMO-TISSUE', 17400::bigint, 14), ('NAVER', 'FUB-DEMO-TISSUE', 17600::bigint, 7), ('NAVER', 'FUB-DEMO-TISSUE', 17780::bigint, 0),
    ('NAVER', 'FUB-DEMO-COFFEE', 5500::bigint, 28), ('NAVER', 'FUB-DEMO-COFFEE', 5300::bigint, 21), ('NAVER', 'FUB-DEMO-COFFEE', 5100::bigint, 14), ('NAVER', 'FUB-DEMO-COFFEE', 4900::bigint, 7), ('NAVER', 'FUB-DEMO-COFFEE', 4770::bigint, 0),
    ('NAVER', 'FUB-DEMO-ICEBOX', 31900::bigint, 28), ('NAVER', 'FUB-DEMO-ICEBOX', 30200::bigint, 21), ('NAVER', 'FUB-DEMO-ICEBOX', 24900::bigint, 14), ('NAVER', 'FUB-DEMO-ICEBOX', 28400::bigint, 7), ('NAVER', 'FUB-DEMO-ICEBOX', 27900::bigint, 0),
    ('NAVER', 'FUB-DEMO-GRILL', 57900::bigint, 28), ('NAVER', 'FUB-DEMO-GRILL', 54900::bigint, 21), ('NAVER', 'FUB-DEMO-GRILL', 48900::bigint, 14), ('NAVER', 'FUB-DEMO-GRILL', 51500::bigint, 7), ('NAVER', 'FUB-DEMO-GRILL', 49800::bigint, 0),
    ('ELEVENST', 'FUB-DEMO-MOSQUITO', 16900::bigint, 28), ('ELEVENST', 'FUB-DEMO-MOSQUITO', 14900::bigint, 21), ('ELEVENST', 'FUB-DEMO-MOSQUITO', 11900::bigint, 14), ('ELEVENST', 'FUB-DEMO-MOSQUITO', 13400::bigint, 7), ('ELEVENST', 'FUB-DEMO-MOSQUITO', 12600::bigint, 0),
    ('ELEVENST', 'FUB-DEMO-TRAVEL', 14900::bigint, 28), ('ELEVENST', 'FUB-DEMO-TRAVEL', 14200::bigint, 21), ('ELEVENST', 'FUB-DEMO-TRAVEL', 12900::bigint, 14), ('ELEVENST', 'FUB-DEMO-TRAVEL', 13800::bigint, 7), ('ELEVENST', 'FUB-DEMO-TRAVEL', 13500::bigint, 0),
    ('NAVER', 'FUB-DEMO-DEODORIZER', 59900::bigint, 28), ('NAVER', 'FUB-DEMO-DEODORIZER', 57900::bigint, 21), ('NAVER', 'FUB-DEMO-DEODORIZER', 52900::bigint, 14), ('NAVER', 'FUB-DEMO-DEODORIZER', 55900::bigint, 7), ('NAVER', 'FUB-DEMO-DEODORIZER', 54560::bigint, 0)
)
INSERT INTO tb_fy_product_price_history (
  offer_sq, price, shipping_fee, total_price, collected_at, create_date, modified_date
)
SELECT
  offer.offer_sq, price_seed.price, 0, price_seed.price,
  CURRENT_TIMESTAMP - make_interval(days => price_seed.days_ago),
  CURRENT_TIMESTAMP - make_interval(days => price_seed.days_ago),
  CURRENT_TIMESTAMP - make_interval(days => price_seed.days_ago)
FROM price_seed
JOIN tb_fy_product_offer offer
  ON offer.provider = price_seed.provider
 AND offer.external_product_id = price_seed.external_product_id;

WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
)
DELETE FROM tb_fy_purchase_click click
USING demo_user
WHERE click.user_sq = demo_user.user_sq
  AND click.external_product_id LIKE 'FUB-DEMO-%';

WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
), click_seed(provider, external_product_id, days_ago, source_context) AS (
  VALUES
    ('NAVER', 'FUB-DEMO-DOG-FOOD', 45, 'SEARCH'),
    ('NAVER', 'FUB-DEMO-DOG-FOOD', 17, 'AGENT_CHAT'),
    ('NAVER', 'FUB-DEMO-DOG-TREAT', 31, 'SEARCH'),
    ('NAVER', 'FUB-DEMO-DOG-TREAT', 9, 'HOME_DEAL'),
    ('NAVER', 'FUB-DEMO-DETERGENT', 37, 'HOME_HOT'),
    ('NAVER', 'FUB-DEMO-DETERGENT', 4, 'AGENT_CHAT'),
    ('NAVER', 'FUB-DEMO-TISSUE', 26, 'HOME_DEAL'),
    ('NAVER', 'FUB-DEMO-COFFEE', 22, 'SEARCH'),
    ('NAVER', 'FUB-DEMO-COFFEE', 6, 'HOME_HOT'),
    ('NAVER', 'FUB-DEMO-GRILL', 19, 'CALENDAR'),
    ('NAVER', 'FUB-DEMO-DEODORIZER', 12, 'HOME_DEAL'),
    ('ELEVENST', 'FUB-DEMO-TRAVEL', 2, 'AGENT_CHAT')
)
INSERT INTO tb_fy_purchase_click (
  user_sq, product_sq, offer_sq, provider, external_product_id, target_url,
  price_at_click, source_context, clicked_at, create_date, modified_date
)
SELECT
  demo_user.user_sq, offer.product_sq, offer.offer_sq, offer.provider,
  offer.external_product_id, offer.product_url, offer.total_price,
  click_seed.source_context,
  CURRENT_TIMESTAMP - make_interval(days => click_seed.days_ago),
  CURRENT_TIMESTAMP - make_interval(days => click_seed.days_ago),
  CURRENT_TIMESTAMP - make_interval(days => click_seed.days_ago)
FROM demo_user
CROSS JOIN click_seed
JOIN tb_fy_product_offer offer
  ON offer.provider = click_seed.provider
 AND offer.external_product_id = click_seed.external_product_id;

-- Replace the earlier compact demo calendar so the briefing has no duplicate scenes.
WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
)
DELETE FROM tb_fy_calendar_event event
USING demo_user
WHERE event.user_sq = demo_user.user_sq
  AND event.provider = 'FUB_DEMO';

WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
), event_seed(external_event_id, title, description, location, start_days, start_time, duration_hours, all_day) AS (
  VALUES
    ('FUB-DEMO-CAMPING', '양평 주말 캠핑', '가족과 1박 2일 오토캠핑', '양평 캠핑장', 3, TIME '10:00', 30, FALSE),
    ('FUB-DEMO-PET-CHECKUP', '보리 정기 건강검진', '피부 상태와 사료 상담', '동네 동물병원', 7, TIME '15:00', 1, FALSE),
    ('FUB-DEMO-HOME-CAFE', '친구들과 홈카페 모임', '커피와 간단한 디저트 준비', '우리 집', 11, TIME '14:00', 3, FALSE),
    ('FUB-DEMO-JEJU', '제주 가족여행', '2박 3일 가족여행', '제주도', 18, TIME '08:00', 58, FALSE),
    ('FUB-DEMO-WORKSHOP', '팀 워크숍', '야외 프로그램이 포함된 팀 워크숍', '가평', 24, TIME '09:00', 10, FALSE),
    ('FUB-DEMO-HOUSEHOLD', '월간 생활용품 장보기', '세제와 화장지 재고 확인', '온라인', 28, TIME '19:00', 1, FALSE)
)
INSERT INTO tb_fy_calendar_event (
  user_sq, provider, provider_calendar_id, external_event_id, title, description,
  location, starts_at, ends_at, all_day, status, last_synced_at,
  create_date, modified_date
)
SELECT
  demo_user.user_sq, 'DEMO', 'fub-demo', event_seed.external_event_id,
  event_seed.title, event_seed.description, event_seed.location,
  CURRENT_DATE + event_seed.start_days + event_seed.start_time,
  CURRENT_DATE + event_seed.start_days + event_seed.start_time
    + make_interval(hours => event_seed.duration_hours),
  event_seed.all_day, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_user
CROSS JOIN event_seed
ON CONFLICT (user_sq, provider, provider_calendar_id, external_event_id) DO UPDATE SET
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  location = EXCLUDED.location,
  starts_at = EXCLUDED.starts_at,
  ends_at = EXCLUDED.ends_at,
  all_day = EXCLUDED.all_day,
  status = EXCLUDED.status,
  last_synced_at = EXCLUDED.last_synced_at,
  modified_date = CURRENT_TIMESTAMP;

WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
)
DELETE FROM tb_fy_calendar_item_suggestion suggestion
USING tb_fy_calendar_event event, demo_user
WHERE suggestion.calendar_event_sq = event.calendar_event_sq
  AND event.user_sq = demo_user.user_sq
  AND event.provider = 'DEMO'
  AND event.provider_calendar_id = 'fub-demo';

WITH demo_user AS (
  SELECT user_sq FROM tb_fy_user WHERE email = 'lion4464@naver.com'
), suggestion_seed(
  external_event_id, keyword, reason, priority, buy_days, provider, external_product_id
) AS (
  VALUES
    ('FUB-DEMO-CAMPING', '캠핑 아이스박스', '가족 캠핑 식재료를 신선하게 보관하려면 필요해요.', 'HIGH', 1, 'NAVER', 'FUB-DEMO-ICEBOX'),
    ('FUB-DEMO-CAMPING', '캠핑 그릴', '저녁 바비큐 일정에 맞춰 미리 준비해 두세요.', 'HIGH', 1, 'NAVER', 'FUB-DEMO-GRILL'),
    ('FUB-DEMO-CAMPING', '캠핑 모기장', '여름 야외 숙박의 벌레 대비 용품이에요.', 'MEDIUM', 1, 'ELEVENST', 'FUB-DEMO-MOSQUITO'),
    ('FUB-DEMO-PET-CHECKUP', '민감성 강아지 사료', '검진 후 사료가 부족하지 않도록 재구매 시점을 맞췄어요.', 'HIGH', 5, 'NAVER', 'FUB-DEMO-DOG-FOOD'),
    ('FUB-DEMO-PET-CHECKUP', '강아지 덴탈 간식', '검진 전후 구강 관리 루틴에 필요한 간식이에요.', 'MEDIUM', 5, 'NAVER', 'FUB-DEMO-DOG-TREAT'),
    ('FUB-DEMO-HOME-CAFE', '커피', '홈카페 모임 인원에 맞춘 음료 준비 품목이에요.', 'HIGH', 8, 'NAVER', 'FUB-DEMO-COFFEE'),
    ('FUB-DEMO-JEJU', '여행용 빨래줄', '가족여행 중 간단한 세탁과 건조에 유용해요.', 'MEDIUM', 14, 'ELEVENST', 'FUB-DEMO-TRAVEL'),
    ('FUB-DEMO-WORKSHOP', '실내 탈취제', '워크숍 숙소에서 함께 쓰기 좋은 생활용품이에요.', 'LOW', 21, 'NAVER', 'FUB-DEMO-DEODORIZER'),
    ('FUB-DEMO-HOUSEHOLD', '캡슐 세탁세제', '최근 구매 주기상 다음 장보기 전에 재고가 부족할 수 있어요.', 'HIGH', 25, 'NAVER', 'FUB-DEMO-DETERGENT'),
    ('FUB-DEMO-HOUSEHOLD', '화장지 30롤', '3인 가구의 월간 소비량을 반영한 재구매 추천이에요.', 'HIGH', 25, 'NAVER', 'FUB-DEMO-TISSUE')
)
INSERT INTO tb_fy_calendar_item_suggestion (
  calendar_event_sq, user_sq, keyword, reason, priority, recommended_buy_by,
  product_sq, offer_sq, product_name, provider, provider_code, product_url,
  image_url, price, product_live, status, generated_at, create_date, modified_date
)
SELECT
  event.calendar_event_sq, demo_user.user_sq, suggestion_seed.keyword,
  suggestion_seed.reason, suggestion_seed.priority,
  CURRENT_DATE + suggestion_seed.buy_days,
  product.product_sq, offer.offer_sq, product.name, offer.provider,
  offer.external_product_id, offer.product_url, product.image_url,
  offer.total_price, TRUE, 'ACTIVE', CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_user
CROSS JOIN suggestion_seed
JOIN tb_fy_calendar_event event
  ON event.user_sq = demo_user.user_sq
 AND event.provider = 'DEMO'
 AND event.provider_calendar_id = 'fub-demo'
 AND event.external_event_id = suggestion_seed.external_event_id
JOIN tb_fy_product_offer offer
  ON offer.provider = suggestion_seed.provider
 AND offer.external_product_id = suggestion_seed.external_product_id
JOIN tb_fy_product product ON product.product_sq = offer.product_sq;

COMMIT;
