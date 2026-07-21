const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const ROOT = path.resolve(__dirname, '..')
const OUTPUT_DIR = path.join(ROOT, 'demo-output', 'raw')
const OUTPUT_FILE = path.join(ROOT, 'demo-output', 'FUB-demo.webm')
const CHROME = path.join(
  process.env.HOME,
  'Library/Caches/ms-playwright/chromium-1217/chrome-mac-arm64',
  'Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing',
)
const BASE_URL = 'http://localhost:3500'
const DEMO_EMAIL = process.env.FUB_DEMO_SIGNUP_EMAIL || 'demo@fub.kr'
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
          font: 600 16px/1.4 system-ui,sans-serif; opacity: 0; transform: translateY(8px);
          transition: opacity .25s ease, transform .25s ease; box-shadow: 0 8px 24px rgba(15,23,42,.22); }
        #fub-demo-chapter.visible { opacity: 1; transform: translateY(0); }
        @keyframes fub-click { from { box-shadow: 0 0 0 0 rgba(6,182,212,.65); }
          to { box-shadow: 0 0 0 22px rgba(6,182,212,0); } }
      `
      const cursor = document.createElement('div')
      cursor.id = 'fub-demo-cursor'
      cursor.style.left = '720px'
      cursor.style.top = '450px'
      const chapter = document.createElement('div')
      chapter.id = 'fub-demo-chapter'
      document.head.appendChild(style)
      document.body.append(cursor, chapter)
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

  await page.goto(`${BASE_URL}/app/my`, { waitUntil: 'domcontentloaded' })
  await page.getByText('활성 위키 8건').waitFor()
  await chapter('1. 개인 위키로 이해한 나의 생활')
  await sleep(1800)
  await page.evaluate(() => window.scrollTo({ top: 520, behavior: 'smooth' }))
  await sleep(1800)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  await sleep(900)

  await click(page.getByRole('link', { name: 'AI 브리핑' }).first(), 800)
  await page.getByRole('heading', { name: '내 일정 준비' }).waitFor()
  await chapter('2. 일정과 가격을 함께 보는 AI 브리핑')
  await sleep(1500)
  await page.getByRole('heading', { name: '지금 가격이 좋은 상품' }).scrollIntoViewIfNeeded()
  await sleep(1600)
  await page.getByRole('heading', { name: '조금 기다려볼 상품' }).scrollIntoViewIfNeeded()
  await sleep(2000)

  const icebox = page.getByRole('link', { name: /클리프타운 아이스박스/ }).first()
  await click(icebox, 700)
  await page.getByRole('heading', { name: /클리프타운 아이스박스/ }).waitFor()
  await chapter('3. 추천 상품 상세와 판매처 연결')
  await sleep(1800)
  const sellerButton = page.getByRole('button', { name: '판매처로 이동' })
  await sellerButton.scrollIntoViewIfNeeded()
  await sellerButton.hover()
  await sleep(1400)

  await click(page.getByRole('button', { name: 'AI 타이밍 판단' }), 900)
  await page.getByRole('heading', { name: '내 일정 준비' }).waitFor()
  await click(page.getByRole('button', { name: '로그아웃' }), 1000)
  await page.getByRole('button', { name: '무료로 시작하기' }).waitFor()
  await chapter('4. 새 사용자 가입과 개인화 시작')
  await click(page.getByRole('button', { name: '무료로 시작하기' }), 700)

  await fill(page.getByLabel('이름'), '김하늘')
  await fill(page.getByLabel('이메일'), DEMO_EMAIL)
  await fill(page.getByLabel('비밀번호'), DEMO_PASSWORD)
  await click(page.getByRole('button', { name: /개인화 설정하기/ }), 600)

  await chapter('5. 여섯 단계로 만드는 나의 위키', 1400)
  await click(page.getByRole('button', { name: '고양이', exact: true }), 350)
  await click(page.getByRole('button', { name: '다음', exact: true }), 450)
  await click(page.getByRole('button', { name: '2명', exact: true }), 350)
  await click(page.getByRole('button', { name: '다음', exact: true }), 450)
  await click(page.getByRole('button', { name: '건강식', exact: true }), 250)
  await click(page.getByRole('button', { name: '디저트', exact: true }), 350)
  await click(page.getByRole('button', { name: '다음', exact: true }), 450)
  await click(page.getByRole('button', { name: '주 1~2회', exact: true }), 350)
  await click(page.getByRole('button', { name: '다음', exact: true }), 450)
  await click(page.getByRole('button', { name: '여행', exact: true }), 250)
  await click(page.getByRole('button', { name: '홈카페·요리', exact: true }), 350)
  await click(page.getByRole('button', { name: '다음', exact: true }), 450)
  await fill(
    page.getByPlaceholder(/평일에는 빠른 배송/),
    '주말 홈카페 모임 전에 커피와 디저트를 준비하고, 여행용품은 최저가일 때 알려주세요.',
  )
  await sleep(900)
  await click(page.getByRole('button', { name: '가입 완료' }), 1000)
  await page.getByRole('heading', { name: '가입 완료' }).waitFor()
  await sleep(1500)
  await click(page.locator('.swal2-confirm'), 800)
  await page.getByText(/김하늘님, 오늘의 구매 타이밍/).waitFor()
  await chapter('6. 개인 위키를 반영한 새로운 추천')
  await sleep(2500)

  await click(page.getByRole('link', { name: '검색' }).first(), 900)
  await page.getByText('내 구매·위키 기반 추천').waitFor()
  await page.getByRole('heading', { name: '커피' }).waitFor()
  await sleep(3000)
  await chapter('FUB 데모 완료 · 필요한 순간보다 먼저 준비합니다', 2200)

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
