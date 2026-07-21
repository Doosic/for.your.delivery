const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const ROOT = path.resolve(__dirname, '..')
const OUTPUT_DIR = path.join(ROOT, 'demo-output', 'raw')
const OUTPUT_FILE = path.join(ROOT, 'demo-output', 'FUB-demo.webm')
const CHROME = process.env.PLAYWRIGHT_CHROME || path.join(
  process.env.HOME,
  'Library/Caches/ms-playwright/chromium-1217/chrome-mac-arm64',
  'Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing',
)
const BASE_URL = 'http://localhost:3500'
const DEMO_EMAIL = process.env.FUB_DEMO_SIGNUP_EMAIL
  || `haneul.demo.${Date.now()}@fub.kr`
const DEMO_PASSWORD = process.env.FUB_DEMO_SIGNUP_PASSWORD || 'FubDemo123!'

const env = Object.fromEntries(
  fs.readFileSync(path.join(ROOT, '.env'), 'utf8')
    .split(/\r?\n/)
    .filter((line) => line && !line.startsWith('#') && line.includes('='))
    .map((line) => {
      const index = line.indexOf('=')
      return [line.slice(0, index), line.slice(index + 1)]
    }),
)

const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url')
const signToken = (claims, minutes) => {
  const header = encode({ alg: 'HS512', typ: 'JWT' })
  const payload = encode({ ...claims, exp: Math.floor(Date.now() / 1000) + minutes * 60 })
  const signature = crypto
    .createHmac('sha512', env.JWT_REAL_KEY)
    .update(`${header}.${payload}`)
    .digest('base64url')
  return `${header}.${payload}.${signature}`
}

const sleep = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds))

async function main() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME,
    args: ['--disable-dev-shm-usage'],
  })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'ko-KR',
    colorScheme: 'light',
    recordVideo: {
      dir: OUTPUT_DIR,
      size: { width: 1440, height: 900 },
    },
  })

  const accessToken = signToken({
    userSq: 2,
    email: 'lion4464@naver.com',
    name: '이승열',
    status: 'USE',
    jti: 'lion4464@naver.com',
  }, 60)
  const refreshToken = signToken({ jti: 'lion4464@naver.com' }, 120)
  await context.addCookies([
    { name: 'DeliveryAuthAccess', value: accessToken, domain: 'localhost', path: '/', httpOnly: true },
    { name: 'DeliveryAuthRefresh', value: refreshToken, domain: 'localhost', path: '/', httpOnly: true },
  ])

  await context.addInitScript(() => {
    const mountDemoUi = () => {
      if (!document.body || document.querySelector('#fub-demo-cursor')) return
      const style = document.createElement('style')
      style.textContent = `
        #fub-demo-cursor { position: fixed; z-index: 2147483647; width: 18px; height: 18px;
          border: 3px solid #06b6d4; border-radius: 50%; background: rgba(255,255,255,.9);
          pointer-events: none; transform: translate(-50%,-50%); box-shadow: 0 2px 8px rgba(15,23,42,.25); }
        #fub-demo-cursor.clicking { animation: fub-click .45s ease-out; }
        #fub-demo-chapter { position: fixed; z-index: 2147483646; left: 28px; bottom: 28px;
          border-radius: 8px; background: rgba(15,23,42,.92); color: white; padding: 12px 18px;
          max-width: 560px; white-space: pre-line; font: 600 16px/1.5 system-ui,sans-serif;
          opacity: 0; transform: translateY(8px);
          transition: opacity .25s ease, transform .25s ease; box-shadow: 0 8px 24px rgba(15,23,42,.22); }
        #fub-demo-chapter.visible { opacity: 1; transform: translateY(0); }
        #fub-demo-title { position: fixed; inset: 0; z-index: 2147483645; display: grid; place-items: center;
          background: #0f172a; color: white; opacity: 0; pointer-events: none; transition: opacity .4s ease; }
        #fub-demo-title.visible { opacity: 1; }
        #fub-demo-title > div { width: min(760px, calc(100vw - 80px)); text-align: center; }
        #fub-demo-title strong { display: block; color: #67e8f9; font: 800 58px/1 system-ui,sans-serif; }
        #fub-demo-title h1 { margin: 24px 0 0; font: 750 34px/1.25 system-ui,sans-serif; }
        #fub-demo-title p { margin: 18px auto 0; max-width: 660px; color: #cbd5e1;
          font: 500 19px/1.65 system-ui,sans-serif; white-space: pre-line; }
        @keyframes fub-click { from { box-shadow: 0 0 0 0 rgba(6,182,212,.65); }
          to { box-shadow: 0 0 0 22px rgba(6,182,212,0); } }
      `
      const cursor = document.createElement('div')
      cursor.id = 'fub-demo-cursor'
      cursor.style.left = '720px'
      cursor.style.top = '450px'
      const chapter = document.createElement('div')
      chapter.id = 'fub-demo-chapter'
      const title = document.createElement('div')
      title.id = 'fub-demo-title'
      title.innerHTML = '<div><strong>FUB</strong><h1></h1><p></p></div>'
      document.head.appendChild(style)
      document.body.append(cursor, chapter, title)
      window.addEventListener('mousemove', (event) => {
        cursor.style.left = `${event.clientX}px`
        cursor.style.top = `${event.clientY}px`
      })
      window.addEventListener('mousedown', () => {
        cursor.classList.remove('clicking')
        void cursor.offsetWidth
        cursor.classList.add('clicking')
      })
    }
    document.addEventListener('DOMContentLoaded', mountDemoUi)
  })

  const page = await context.newPage()
  page.setDefaultTimeout(20_000)
  const video = page.video()

  const chapter = async (text, duration = 1900) => {
    await page.evaluate((message) => {
      const element = document.querySelector('#fub-demo-chapter')
      if (!element) return
      element.textContent = message
      element.classList.add('visible')
    }, text)
    await sleep(duration)
    await page.evaluate(() => document.querySelector('#fub-demo-chapter')?.classList.remove('visible'))
    await sleep(300)
  }

  const titleCard = async (title, subtitle, duration = 4000) => {
    await page.evaluate(({ heading, description }) => {
      const element = document.querySelector('#fub-demo-title')
      if (!element) return
      element.querySelector('h1').textContent = heading
      element.querySelector('p').textContent = description
      element.classList.add('visible')
    }, { heading: title, description: subtitle })
    await sleep(duration)
    await page.evaluate(() => document.querySelector('#fub-demo-title')?.classList.remove('visible'))
    await sleep(500)
  }

  const click = async (locator, pause = 900) => {
    await locator.scrollIntoViewIfNeeded()
    const box = await locator.boundingBox()
    if (box) {
      await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2, { steps: 18 })
      await sleep(350)
    }
    await locator.click()
    await sleep(pause)
  }

  const fill = async (locator, value) => {
    await locator.scrollIntoViewIfNeeded()
    await locator.click()
    await locator.fill('')
    await locator.pressSequentially(value, { delay: 42 })
    await sleep(300)
  }

  await page.goto(`${BASE_URL}/app/main`, { waitUntil: 'domcontentloaded' })
  await page.getByText(/오늘의 구매 타이밍/).waitFor()
  await titleCard(
    '필요해지기 전에 먼저 준비하는 쇼핑',
    '생활 맥락과 가격 흐름을 함께 읽는 선행구매 AI Agent',
    5000,
  )

  await chapter('1. 로그인과 동시에 시작되는 개인화 홈\n실상품과 오늘의 구매 타이밍을 한눈에 확인합니다.', 2400)
  await sleep(2800)
  await page.getByRole('heading', { name: '지금 핫한 상품' }).scrollIntoViewIfNeeded()
  await sleep(2500)
  await page.getByRole('heading', { name: '역대가 도달' }).scrollIntoViewIfNeeded()
  await sleep(2500)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await sleep(1200)

  await click(page.getByRole('button', { name: '이승열님' }), 500)
  await click(page.getByRole('menuitem', { name: '마이페이지' }), 900)
  await page.getByRole('heading', { name: '내 정보와 FUB의 기억' }).waitFor()
  await chapter('2. 사용자가 직접 통제하는 개인 위키\n가구·반려생활·취미·쇼핑 성향을 추천 근거로 사용합니다.', 2500)
  await sleep(2600)
  await page.evaluate(() => window.scrollTo({ top: 430, behavior: 'smooth' }))
  await sleep(2500)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await sleep(1200)

  await click(page.getByRole('link', { name: '검색' }).first(), 900)
  await page.getByText('내 구매·위키 기반 추천').waitFor()
  await chapter('3. 위키와 구매 이력을 반영한 실상품 검색\n최저가·인기·구매 행동 기준을 즉시 바꿔 비교합니다.', 2500)
  await sleep(2500)
  await click(page.getByRole('button', { name: '인기순' }), 2300)
  await click(page.getByRole('button', { name: '최저가순' }), 1800)
  await fill(page.getByPlaceholder('상품명을 입력하세요'), '캠핑용 아이스박스')
  await click(page.getByRole('button', { name: '검색', exact: true }), 2600)
  await page.getByRole('heading', { name: '캠핑용 아이스박스' }).waitFor()
  await page.locator('a[href^="/app/products/"]').first().waitFor({ timeout: 12000 })
  await sleep(5200)

  await click(page.getByRole('link', { name: '캘린더' }).first(), 900)
  await page.getByRole('heading', { name: '다가오는 일정', exact: true }).waitFor()
  await page.getByRole('heading', { name: '다가오는 일정 목록', exact: true }).waitFor()
  await chapter('4. 일정에서 필요한 준비물을 선제적으로 발견\n행사 날짜와 배송 여유를 계산해 주문 마감일을 제안합니다.', 2600)
  await sleep(3500)
  await page.evaluate(() => window.scrollTo({ top: 360, behavior: 'smooth' }))
  await sleep(2500)

  await click(page.getByRole('link', { name: 'AI 브리핑' }).first(), 900)
  await page.getByRole('heading', { name: '내 일정 준비' }).waitFor()
  await chapter('5. 일정·가격·개인 위키를 한 화면에서 판단\n지금 살 상품과 기다릴 상품을 근거와 함께 나눕니다.', 2600)
  await sleep(2500)
  await page.getByRole('heading', { name: '지금 가격이 좋은 상품' }).scrollIntoViewIfNeeded()
  await sleep(2800)
  await page.getByRole('heading', { name: '조금 기다려볼 상품' }).scrollIntoViewIfNeeded()
  await sleep(3000)

  await fill(page.getByPlaceholder('메시지를 입력하세요'), '이번 주말 캠핑 전에 지금 사야 할 것만 알려줘')
  await click(page.getByRole('button', { name: '보내기' }), 700)
  await chapter('6. 브리핑에서 바로 이어지는 쇼핑 대화\nAgent가 같은 개인화 맥락으로 상품과 구매 시점을 다시 좁힙니다.', 2500)
  await sleep(7000)
  await page.locator('div.overflow-y-auto').first().evaluate((element) => {
    element.scrollTo({ top: element.scrollHeight, behavior: 'smooth' })
  })
  await sleep(3200)

  const icebox = page.getByRole('link', { name: /클리프타운 아이스박스/ }).first()
  await click(icebox, 900)
  await page.getByRole('heading', { name: /클리프타운 아이스박스/ }).waitFor()
  await chapter('7. 실상품 상세와 AI 요약, 실제 판매처 연결\n추천 이유를 확인하고 구매 행동까지 자연스럽게 이어집니다.', 2600)
  await sleep(2800)
  await page.getByRole('heading', { name: '상품 정보 요약' }).scrollIntoViewIfNeeded()
  await sleep(3200)
  const sellerButton = page.getByRole('button', { name: '판매처로 이동' })
  await sellerButton.scrollIntoViewIfNeeded()
  await sellerButton.hover()
  await sleep(2600)

  await click(page.getByRole('link', { name: '구매목록' }).first(), 900)
  await page.getByRole('heading', { name: '구매목록 가져오기' }).waitFor()
  await chapter('8. 구매 행동을 다시 개인화 데이터로 연결\n확인된 구매목록은 다음 재구매 시점과 추천 정확도를 높입니다.', 2500)
  await sleep(4500)

  await click(page.getByRole('button', { name: '이승열님' }), 450)
  await click(page.getByRole('menuitem', { name: '로그아웃' }), 1000)
  await page.getByRole('button', { name: '무료로 시작하기' }).waitFor()
  await chapter('9. 신규 사용자는 간단한 질문으로 개인화를 시작합니다.', 2200)
  await sleep(1800)
  await click(page.getByRole('button', { name: '무료로 시작하기' }), 700)

  await fill(page.getByLabel('이름'), '김하늘')
  await fill(page.getByLabel('이메일'), DEMO_EMAIL)
  await fill(page.getByLabel('비밀번호'), DEMO_PASSWORD)
  await click(page.getByRole('button', { name: /개인화 설정하기/ }), 600)

  await chapter('10. 여섯 단계로 완성되는 사용자 주도 개인 위키', 2200)
  await click(page.getByRole('button', { name: '고양이', exact: true }), 500)
  await click(page.getByRole('button', { name: '다음', exact: true }), 600)
  await click(page.getByRole('button', { name: '2명', exact: true }), 500)
  await click(page.getByRole('button', { name: '다음', exact: true }), 600)
  await click(page.getByRole('button', { name: '건강식', exact: true }), 450)
  await click(page.getByRole('button', { name: '디저트', exact: true }), 500)
  await click(page.getByRole('button', { name: '다음', exact: true }), 600)
  await click(page.getByRole('button', { name: '주 1~2회', exact: true }), 500)
  await click(page.getByRole('button', { name: '다음', exact: true }), 600)
  await click(page.getByRole('button', { name: '여행', exact: true }), 450)
  await click(page.getByRole('button', { name: '홈카페·요리', exact: true }), 500)
  await click(page.getByRole('button', { name: '다음', exact: true }), 600)
  await fill(
    page.getByPlaceholder(/평일에는 빠른 배송/),
    '주말 홈카페 모임 전에 커피와 디저트를 준비하고, 여행용품은 최저가일 때 알려주세요.',
  )
  await sleep(1800)
  await click(page.getByRole('button', { name: '가입 완료' }), 1400)
  await page.getByRole('heading', { name: '가입 완료' }).waitFor()
  await sleep(1700)
  await click(page.locator('.swal2-confirm'), 800)
  await page.getByText(/김하늘님, 오늘의 구매 타이밍/).waitFor()
  await chapter('11. 가입 직후부터 달라지는 개인화 홈\n방금 선택한 취미와 소비 성향이 바로 추천에 반영됩니다.', 2600)
  await sleep(3500)

  await click(page.getByRole('link', { name: '검색' }).first(), 900)
  await page.getByText('내 구매·위키 기반 추천').waitFor()
  await chapter('12. 같은 검색 화면도 사용자에 따라 다른 시작점', 2200)
  await sleep(3200)

  await click(page.getByRole('button', { name: '김하늘님' }), 450)
  await click(page.getByRole('menuitem', { name: '마이페이지' }), 900)
  await page.getByRole('heading', { name: '내 정보와 FUB의 기억' }).waitFor()
  await sleep(3200)
  await titleCard(
    'FUB · 먼저 준비하는 쇼핑',
    '생활 맥락을 이해하고, 필요한 물건을 가장 좋은 순간에 연결합니다.',
    5000,
  )

  await context.close()
  await browser.close()
  const rawVideo = await video.path()
  fs.copyFileSync(rawVideo, OUTPUT_FILE)
  process.stdout.write(`${OUTPUT_FILE}\n`)
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
