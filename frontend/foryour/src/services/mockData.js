export const productMocks = [
  {
    productSq: 'LOCAL-NAVER-1',
    name: '오리진 캣 6가지 생선 1.8kg',
    price: 38900,
    mallName: '네이버쇼핑',
    source: 'NAVER',
    searchTerms: ['고양이', '고양이 사료', '사료', '오리진'],
    imageUrl: '',
    productUrl:
      'https://search.shopping.naver.com/search/all?query=%EC%98%A4%EB%A6%AC%EC%A7%84%20%EC%BA%A3',
    providerCode: 'LOCAL_FALLBACK',
  },
  {
    productSq: 'LOCAL-11ST-1',
    name: '로얄캐닌 인도어 2kg',
    price: 29800,
    mallName: '11번가',
    source: 'ELEVENST',
    searchTerms: ['고양이', '고양이 사료', '사료', '로얄캐닌'],
    imageUrl: '',
    productUrl:
      'https://search.11st.co.kr/Search.tmall?kwd=%EB%A1%9C%EC%96%84%EC%BA%90%EB%8B%8C',
    providerCode: 'LOCAL_FALLBACK',
  },
  {
    productSq: 'LOCAL-NAVER-2',
    name: '고양이 모래 벤토나이트 6L',
    price: 7900,
    mallName: '네이버쇼핑',
    source: 'NAVER',
    searchTerms: ['고양이', '고양이 모래', '모래', '벤토나이트'],
    imageUrl: '',
    productUrl:
      'https://search.shopping.naver.com/search/all?query=%EA%B3%A0%EC%96%91%EC%9D%B4%20%EB%AA%A8%EB%9E%98',
    providerCode: 'LOCAL_FALLBACK',
  },
  {
    productSq: 'LOCAL-11ST-2',
    name: '츄르 버라이어티 20개입',
    price: 12500,
    mallName: '11번가',
    source: 'ELEVENST',
    searchTerms: ['고양이', '고양이 간식', '간식', '츄르'],
    imageUrl: '',
    productUrl: 'https://search.11st.co.kr/Search.tmall?kwd=%EC%B8%84%EB%A5%B4',
    providerCode: 'LOCAL_FALLBACK',
  },
];

export const briefingMock = {
  summary:
    '구매목록과 캘린더 연결 상태를 기준으로 오늘 살 상품과 기다릴 상품을 나눠 보여줍니다.',
  sections: [
    {
      title: '오늘 살 것',
      items: [
        {
          productSq: 'LOCAL-DETERGENT-1',
          name: '세탁세제 리필 2.6L',
          note: '재고 D-3 · 가격 하락 감지',
          decision: 'BUY_NOW',
          label: 'BUY NOW',
          price: 12900,
          mallName: '네이버쇼핑',
          source: 'NAVER',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
        {
          productSq: 'LOCAL-NAVER-2',
          name: '고양이 모래 6L x2',
          note: '정기 구매 주기 도달',
          decision: 'BUY_NOW',
          label: 'BUY NOW',
          price: 7900,
          mallName: '네이버쇼핑',
          source: 'NAVER',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
      ],
    },
    {
      title: '기다릴 것',
      items: [
        {
          productSq: 'LOCAL-NAVER-1',
          name: '고양이 사료 1.5kg',
          note: '다음 주 최저가 예상',
          decision: 'WAIT',
          label: 'WAIT',
          price: 38900,
          mallName: '네이버쇼핑',
          source: 'NAVER',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
      ],
    },
    {
      title: '일정 준비',
      items: [
        {
          productSq: 'LOCAL-CAMPING-GAS',
          name: '부탄가스 4개입',
          note: '캠핑 준비물 추천 · 구매 마감 D-2',
          decision: 'BUY_NOW',
          label: 'BUY NOW',
          price: 8900,
          mallName: '네이버쇼핑',
          source: 'NAVER',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
        {
          productSq: 'LOCAL-CAMPING-ICE',
          name: '아이스팩 대형',
          note: '캠핑 준비물 추천 · 재고 없음',
          decision: 'BUY_NOW',
          label: 'BUY NOW',
          price: 4500,
          mallName: '네이버쇼핑',
          source: 'NAVER',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
        {
          productSq: 'LOCAL-CAMPING-CHARCOAL',
          name: '화로용 숯 3kg',
          note: '캠핑 준비물 추천 · 가격 확인 대기',
          decision: 'WAIT',
          label: 'WAIT',
          price: 12000,
          mallName: '11번가',
          source: 'ELEVENST',
          imageUrl: '',
          productUrl: '',
          providerCode: 'LOCAL_FALLBACK',
        },
      ],
    },
  ],
};

export const agentMock = {
  monitoring: [
    { label: '재구매 모니터링', value: '5개 품목 추적 중' },
    { label: '가격 추적', value: '2건 · 하락 시 알림' },
    { label: '일정 준비', value: '캠핑 일정 · 구매 마감 계산' },
  ],
  decisions: [
    {
      name: '세탁세제 리필 2.6L',
      note: '재고 D-3 · 구매 권장',
      decision: 'BUY_NOW',
      label: 'BUY NOW',
    },
    {
      name: '고양이 사료 1.5kg',
      note: '다음 주 가격 하락 예상',
      decision: 'WAIT',
      label: 'WAIT',
    },
    {
      name: '물티슈 캡형 10팩',
      note: '재고 충분',
      decision: 'HOLD',
      label: 'HOLD',
    },
  ],
};

export const importMock = {
  items: [
    {
      importedItemSq: 1,
      name: '고양이 사료 오리진 1.5kg',
      quantity: 1,
      price: 32400,
      purchasedAt: '2026-07-21',
      source: 'COUPANG',
      isNew: true,
    },
    {
      importedItemSq: 2,
      name: '세탁세제 리필 2.6L',
      quantity: 1,
      price: 12900,
      purchasedAt: '2026-07-20',
      source: 'NAVER',
      isNew: true,
    },
    {
      importedItemSq: 3,
      name: '우유 900ml x2',
      quantity: 2,
      price: 5600,
      purchasedAt: '2026-07-19',
      source: 'NAVER',
      isNew: true,
    },
    {
      importedItemSq: 4,
      name: '물티슈 캡형 10팩',
      quantity: 1,
      price: 9900,
      purchasedAt: '2026-07-18',
      source: 'COUPANG',
      isNew: false,
    },
  ],
  connections: [
    {
      importConnectionSq: 1,
      source: 'NAVER',
      label: '네이버',
      description: '연결됨 · 마지막 동기화 오늘 07:40',
      connected: true,
      autoSync: '매일 06:00',
    },
    {
      importConnectionSq: 2,
      source: 'COUPANG',
      label: '쿠팡',
      description: '연결됨 · 마지막 동기화 오늘 07:40',
      connected: true,
      autoSync: '매일 06:00',
    },
    {
      importConnectionSq: 3,
      source: 'GMAIL',
      label: 'Gmail',
      description: '주문확인 메일에서 자동 수집',
      connected: false,
      action: 'Google 계정 연결',
    },
    {
      importConnectionSq: 4,
      source: 'GOOGLE_CALENDAR',
      label: 'Google Calendar',
      description: '일정에서 준비물과 구매 마감일 추출',
      connected: false,
      action: '캘린더 연결',
    },
  ],
};

export const calendarMock = {
  suggestions: [
    {
      eventSq: 1,
      title: '주말 캠핑',
      startsAt: '2026-07-25 09:00',
      location: '가평',
      suggestion: '모기퇴치제, 아이스박스, 숯은 7/23까지 구매 권장',
    },
    {
      eventSq: 2,
      title: '반려묘 병원 방문',
      startsAt: '2026-07-28 15:30',
      location: '동네 동물병원',
      suggestion: '이동장 패드와 간식 재고 확인',
    },
    {
      eventSq: 3,
      title: '친구 생일',
      startsAt: '2026-08-02 19:00',
      location: '성수',
      suggestion: '선물 후보를 7/30 브리핑에 노출',
    },
  ],
};

export const chatMock = {
  initialMessages: [],
};
