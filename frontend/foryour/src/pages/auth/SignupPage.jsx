import { ArrowLeft, ArrowRight, Check, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import BrandLogo from '@/components/BrandLogo.jsx'
import authService from '@/services/authService.js'
import { wikiService } from '@/services/wikiService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const QUESTIONS = [
  {
    key: 'pets',
    title: '함께 사는 반려동물이 있나요?',
    description: '필요한 사료와 생활용품을 더 알맞게 준비할 수 있어요.',
    options: ['없음', '강아지', '고양이', '새·소동물', '기타'],
    multiple: true,
  },
  {
    key: 'householdSize',
    title: '가구원은 모두 몇 명인가요?',
    description: '소비 속도와 적정 구매 수량을 계산할 때 활용해요.',
    options: [1, 2, 3, 4, 5, 6],
    labels: ['1명', '2명', '3명', '4명', '5명', '6명 이상'],
  },
  {
    key: 'favoriteFoods',
    title: '평소 좋아하는 음식은 무엇인가요?',
    description: '여러 항목을 선택할 수 있어요.',
    options: ['한식', '양식', '중식·아시아식', '건강식', '간편식', '디저트'],
    multiple: true,
  },
  {
    key: 'cookingFrequency',
    title: '직접 요리는 얼마나 자주 하나요?',
    description: '식재료와 간편식 추천 비중을 조절할게요.',
    options: ['거의 안 해요', '주 1~2회', '주 3~4회', '거의 매일'],
  },
  {
    key: 'hobbies',
    title: '즐겨 하는 취미가 있나요?',
    description: '일정과 관심사에 맞는 준비물을 먼저 찾아드려요.',
    options: ['캠핑', '여행', '운동', '홈카페·요리', '게임·독서', '반려생활'],
    multiple: true,
  },
]

const INITIAL_PROFILE = {
  pets: [],
  householdSize: null,
  favoriteFoods: [],
  cookingFrequency: '',
  hobbies: [],
  prompt: '',
}

function SignupPage() {
  const navigate = useNavigate()
  const alert = useAlert()
  const { saveUser } = useAuth()
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [profile, setProfile] = useState(INITIAL_PROFILE)
  const [step, setStep] = useState(0)
  const [accountCreated, setAccountCreated] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateForm = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }))
  }

  const currentQuestion = QUESTIONS[step - 1]
  const isPromptStep = step === QUESTIONS.length + 1
  const totalProfileSteps = QUESTIONS.length + 1

  const selectOption = (question, option) => {
    setProfile((current) => {
      if (!question.multiple) {
        return { ...current, [question.key]: option }
      }
      const selected = current[question.key]
      if (question.key === 'pets' && option === '없음') {
        return { ...current, pets: selected.includes('없음') ? [] : ['없음'] }
      }
      const withoutNone = question.key === 'pets' ? selected.filter((item) => item !== '없음') : selected
      return {
        ...current,
        [question.key]: withoutNone.includes(option)
          ? withoutNone.filter((item) => item !== option)
          : [...withoutNone, option],
      }
    })
  }

  const hasAnswer = () => {
    if (isPromptStep) return true
    const answer = profile[currentQuestion.key]
    return Array.isArray(answer) ? answer.length > 0 : answer !== null && answer !== ''
  }

  const startQuestions = (event) => {
    event.preventDefault()
    setStep(1)
  }

  const completeSignup = async () => {
    try {
      setIsSubmitting(true)
      if (!accountCreated) {
        await authService().signup(form)
        setAccountCreated(true)
      }
      const user = await authService().login(form.email, form.password)
      saveUser(user)
      await wikiService.saveOnboarding(profile)
      await alert.alertSuccess('가입 완료', '선택한 정보를 바탕으로 FUB 개인화를 시작합니다.')
      navigate('/app/main')
    } catch (error) {
      alert.alertWarning('알림', error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  const renderAccountForm = () => (
    <form className="space-y-5" onSubmit={startQuestions}>
      <label className="block">
        <span className="mb-2 block text-sm font-medium text-slate-700">이름</span>
        <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
          <UserRound size={18} className="text-slate-400" aria-hidden="true" />
          <input type="text" className="h-full min-w-0 flex-1 outline-none" placeholder="홍길동" value={form.name} onChange={updateForm('name')} required />
        </span>
      </label>
      <label className="block">
        <span className="mb-2 block text-sm font-medium text-slate-700">이메일</span>
        <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
          <Mail size={18} className="text-slate-400" aria-hidden="true" />
          <input type="email" className="h-full min-w-0 flex-1 outline-none" placeholder="email@example.com" value={form.email} onChange={updateForm('email')} required />
        </span>
      </label>
      <label className="block">
        <span className="mb-2 block text-sm font-medium text-slate-700">비밀번호</span>
        <span className="flex h-12 items-center gap-3 rounded-md border border-slate-300 px-3 focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
          <LockKeyhole size={18} className="text-slate-400" aria-hidden="true" />
          <input type="password" className="h-full min-w-0 flex-1 outline-none" placeholder="8자 이상 입력" value={form.password} onChange={updateForm('password')} minLength={8} required />
        </span>
      </label>
      <button type="submit" className="flex h-12 w-full items-center justify-center gap-2 rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800">
        개인화 설정하기 <ArrowRight size={17} aria-hidden="true" />
      </button>
    </form>
  )

  const renderQuestion = () => (
    <div>
      <div className="mb-7">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-400">
          <span>개인화 설정</span>
          <span>{step} / {totalProfileSteps}</span>
        </div>
        <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
          <div className="h-full rounded-full bg-cyan-600 transition-all" style={{ width: `${(step / totalProfileSteps) * 100}%` }} />
        </div>
      </div>

      <h2 className="text-xl font-semibold text-slate-950">{currentQuestion.title}</h2>
      <p className="mt-2 text-sm leading-6 text-slate-500">{currentQuestion.description}</p>
      <div className="mt-7 grid grid-cols-2 gap-3 sm:grid-cols-3">
        {currentQuestion.options.map((option, index) => {
          const answer = profile[currentQuestion.key]
          const selected = Array.isArray(answer) ? answer.includes(option) : answer === option
          return (
            <button
              key={option}
              type="button"
              onClick={() => selectOption(currentQuestion, option)}
              className={`relative min-h-12 rounded-md border px-3 py-2 text-sm font-medium transition ${selected ? 'border-cyan-600 bg-cyan-50 text-cyan-800 ring-2 ring-cyan-100' : 'border-slate-300 bg-white text-slate-600 hover:border-cyan-300'}`}
            >
              {selected && <Check size={14} className="absolute right-2 top-2" aria-hidden="true" />}
              {currentQuestion.labels?.[index] ?? option}
            </button>
          )
        })}
      </div>
    </div>
  )

  const renderPrompt = () => (
    <div>
      <div className="mb-7 flex items-center justify-between text-xs font-semibold text-slate-400">
        <span>개인화 설정</span>
        <span>{step} / {totalProfileSteps}</span>
      </div>
      <h2 className="text-xl font-semibold text-slate-950">혹시 더 알려주고 싶은 게 있나요?</h2>
      <p className="mt-2 text-sm leading-6 text-slate-500">구매 습관이나 꼭 기억했으면 하는 내용을 자유롭게 적어주세요.</p>
      <textarea
        value={profile.prompt}
        onChange={(event) => setProfile((current) => ({ ...current, prompt: event.target.value }))}
        maxLength={1000}
        rows={6}
        className="mt-6 w-full resize-none rounded-md border border-slate-300 p-4 text-sm leading-6 outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-100"
        placeholder="예: 평일에는 빠른 배송을 선호하고, 캠핑 일정 일주일 전에는 준비물을 알려주세요."
      />
      <p className="mt-1 text-right text-xs text-slate-400">{profile.prompt.length} / 1000</p>
    </div>
  )

  return (
    <section className="w-full max-w-xl rounded-lg border border-slate-200 bg-white p-7 shadow-sm sm:p-10">
      <div className="mb-8">
        <BrandLogo compact />
        {step === 0 && <h1 className="mt-2 text-2xl font-semibold text-slate-950">회원가입</h1>}
      </div>

      {step === 0 ? renderAccountForm() : isPromptStep ? renderPrompt() : renderQuestion()}

      {step > 0 && (
        <div className="mt-8 flex gap-3">
          <button type="button" onClick={() => setStep((current) => current - 1)} disabled={isSubmitting} className="grid h-12 w-12 shrink-0 place-items-center rounded-md border border-slate-300 text-slate-600 hover:bg-slate-50 disabled:opacity-40" aria-label="이전 질문" title="이전">
            <ArrowLeft size={18} aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={() => isPromptStep ? completeSignup() : setStep((current) => current + 1)}
            disabled={!hasAnswer() || isSubmitting}
            className="flex h-12 flex-1 items-center justify-center gap-2 rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300"
          >
            {isPromptStep ? (isSubmitting ? '저장 중...' : '가입 완료') : '다음'}
            {!isPromptStep && <ArrowRight size={17} aria-hidden="true" />}
          </button>
        </div>
      )}

      {step === 0 && (
        <p className="mt-6 text-center text-sm text-slate-600">
          이미 계정이 있나요? <Link to="/app/login" className="font-semibold text-cyan-700 hover:text-cyan-800">로그인</Link>
        </p>
      )}
    </section>
  )
}

export default SignupPage
