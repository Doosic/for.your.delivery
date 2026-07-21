import {
  Archive,
  BookOpenText,
  Check,
  Compass,
  Heart,
  Home,
  PawPrint,
  Pencil,
  Plus,
  Save,
  Search,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  UserRound,
  Utensils,
  X,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { wikiService } from '@/services/wikiService.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

const CATEGORY_META = {
  PROFILE: { label: '기본 정보', icon: UserRound, color: 'bg-cyan-50 text-cyan-700' },
  HOUSEHOLD: { label: '우리 집', icon: Home, color: 'bg-blue-50 text-blue-700' },
  PET: { label: '반려생활', icon: PawPrint, color: 'bg-emerald-50 text-emerald-700' },
  SHOPPING: { label: '쇼핑 성향', icon: ShoppingBag, color: 'bg-rose-50 text-rose-700' },
  FOOD: { label: '음식', icon: Utensils, color: 'bg-amber-50 text-amber-700' },
  LIFESTYLE: { label: '생활 방식', icon: Heart, color: 'bg-pink-50 text-pink-700' },
  HOBBY: { label: '취미', icon: Compass, color: 'bg-indigo-50 text-indigo-700' },
  PREFERENCE: { label: '추가 선호', icon: Sparkles, color: 'bg-violet-50 text-violet-700' },
  OTHER: { label: '기타', icon: BookOpenText, color: 'bg-slate-100 text-slate-600' },
}

const EDIT_CATEGORIES = ['PROFILE', 'HOUSEHOLD', 'PET', 'SHOPPING', 'FOOD', 'LIFESTYLE', 'HOBBY', 'PREFERENCE', 'OTHER']
const EMPTY_FORM = { category: 'PREFERENCE', summary: '' }

const categoryMeta = (category) => CATEGORY_META[category] ?? CATEGORY_META.OTHER

const contentText = (entry) => {
  const content = entry.content ?? {}
  const values = Object.values(content)
    .flatMap((value) => Array.isArray(value) ? value : [value])
    .filter((value) => value !== null && value !== undefined && String(value).trim())
    .map(String)
  if (values.length === 0) return entry.summary
  if (values.length === 1 && values[0] === entry.summary) return entry.summary
  return values.join(', ')
}

const formatDate = (value) => {
  if (!value) return ''
  return new Intl.DateTimeFormat('ko-KR', { month: 'short', day: 'numeric' }).format(new Date(value))
}

function WikiEditor({ entry, form, onChange, onClose, onSubmit, isSaving }) {
  const isEditing = Boolean(entry)

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-slate-950/45 p-4" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose()
    }}>
      <section className="w-full max-w-lg rounded-lg border border-slate-200 bg-white shadow-xl" role="dialog" aria-modal="true" aria-labelledby="wiki-editor-title">
        <header className="flex items-center justify-between border-b border-slate-200 px-5 py-4">
          <div>
            <p className="text-xs font-semibold text-cyan-700">개인 위키</p>
            <h2 id="wiki-editor-title" className="mt-0.5 text-lg font-semibold">{isEditing ? '기억 수정' : '새 기억 추가'}</h2>
          </div>
          <button type="button" onClick={onClose} className="grid size-9 place-items-center rounded-md text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" aria-label="닫기" title="닫기">
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        <form className="space-y-5 p-5" onSubmit={onSubmit}>
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">분류</span>
            <select
              value={form.category}
              onChange={(event) => onChange({ ...form, category: event.target.value })}
              className="h-11 w-full rounded-md border border-slate-300 bg-white px-3 text-sm outline-none focus:border-cyan-600 focus:ring-2 focus:ring-cyan-100"
            >
              {EDIT_CATEGORIES.map((category) => <option key={category} value={category}>{categoryMeta(category).label}</option>)}
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">FUB가 기억할 내용</span>
            <textarea
              value={form.summary}
              onChange={(event) => onChange({ ...form, summary: event.target.value })}
              maxLength={500}
              rows={6}
              autoFocus
              className="w-full resize-none rounded-md border border-slate-300 p-3 text-sm leading-6 outline-none placeholder:text-slate-400 focus:border-cyan-600 focus:ring-2 focus:ring-cyan-100"
              placeholder="예: 강아지 한 마리를 키우고 있어요. 알레르기용 사료를 선호해요."
              required
            />
            <span className="mt-1 block text-right text-xs text-slate-400">{form.summary.length} / 500</span>
          </label>

          <div className="flex gap-2 rounded-md bg-slate-50 p-3 text-xs leading-5 text-slate-500">
            <ShieldCheck size={16} className="mt-0.5 shrink-0 text-emerald-600" aria-hidden="true" />
            연락처와 개인 식별정보는 저장 전에 자동으로 마스킹되며, 이 내용은 내 추천에만 사용됩니다.
          </div>

          <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
            <button type="button" onClick={onClose} className="h-10 rounded-md border border-slate-300 px-4 text-sm font-semibold text-slate-600 transition hover:bg-slate-50">취소</button>
            <button type="submit" disabled={isSaving || !form.summary.trim()} className="flex h-10 items-center gap-2 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:cursor-not-allowed disabled:bg-slate-300">
              <Save size={15} aria-hidden="true" /> {isSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}

function MyPage() {
  const { user } = useAuth()
  const alert = useAlert()
  const [entries, setEntries] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('ALL')
  const [editorEntry, setEditorEntry] = useState(undefined)
  const [form, setForm] = useState(EMPTY_FORM)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    let active = true
    wikiService.getEntries()
      .then((response) => {
        if (active) setEntries(response.entries ?? [])
      })
      .catch((error) => {
        if (active) alert.alertWarning('알림', error.message)
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => {
      active = false
    }
  }, [alert])

  const categories = useMemo(() => {
    const found = [...new Set(entries.map((entry) => entry.category))]
    return ['ALL', ...EDIT_CATEGORIES.filter((item) => found.includes(item))]
  }, [entries])

  const visibleEntries = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    return entries.filter((entry) => {
      if (category !== 'ALL' && entry.category !== category) return false
      if (!normalizedQuery) return true
      return `${entry.summary} ${contentText(entry)} ${categoryMeta(entry.category).label}`.toLowerCase().includes(normalizedQuery)
    })
  }, [category, entries, query])

  const openCreate = () => {
    setEditorEntry(null)
    setForm(EMPTY_FORM)
  }

  const openEdit = (entry) => {
    setEditorEntry(entry)
    setForm({ category: entry.category, summary: contentText(entry) })
  }

  const closeEditor = () => {
    if (isSaving) return
    setEditorEntry(undefined)
  }

  const saveEntry = async (event) => {
    event.preventDefault()
    try {
      setIsSaving(true)
      const saved = editorEntry
        ? await wikiService.updateEntry(editorEntry.wikiEntrySq, form)
        : await wikiService.createEntry(form)
      setEntries((current) => editorEntry
        ? current.map((entry) => entry.wikiEntrySq === saved.wikiEntrySq ? saved : entry)
        : [saved, ...current])
      setEditorEntry(undefined)
      await alert.alertSuccess('저장 완료', '다음 추천부터 수정한 개인 정보를 반영합니다.')
    } catch (error) {
      alert.alertWarning('알림', error.message)
    } finally {
      setIsSaving(false)
    }
  }

  const archiveEntry = async (entry) => {
    const confirmed = await alert.alertConfirm('기억을 보관할까요?', '보관한 내용은 추천에 더 이상 사용되지 않습니다.', '보관', '취소')
    if (!confirmed) return
    try {
      await wikiService.archiveEntry(entry.wikiEntrySq)
      setEntries((current) => current.filter((item) => item.wikiEntrySq !== entry.wikiEntrySq))
    } catch (error) {
      alert.alertWarning('알림', error.message)
    }
  }

  const initials = user?.name?.trim()?.slice(0, 1) || 'F'

  return (
    <div className="space-y-6">
      <header>
        <p className="text-sm font-semibold text-cyan-700">마이페이지</p>
        <h1 className="mt-1 text-2xl font-semibold">내 정보와 FUB의 기억</h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">추천에 사용하는 정보를 직접 확인하고 최신 상태로 관리하세요.</p>
      </header>

      <div className="grid items-start gap-6 lg:grid-cols-[17rem_minmax(0,1fr)]">
        <aside className="space-y-4 lg:sticky lg:top-22">
          <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div className="grid size-14 place-items-center rounded-lg bg-slate-950 text-xl font-semibold text-white">{initials}</div>
            <h2 className="mt-4 text-lg font-semibold">{user?.name}님</h2>
            <p className="mt-1 break-all text-sm text-slate-500">{user?.email}</p>
            <div className="mt-5 border-t border-slate-100 pt-4">
              <p className="flex items-center gap-2 text-sm font-medium text-emerald-700"><Check size={16} aria-hidden="true" /> 개인화 사용 중</p>
              <p className="mt-2 text-xs leading-5 text-slate-500">활성 위키 {entries.length}건을 검색과 AI 브리핑에 반영합니다.</p>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <p className="flex items-center gap-2 text-sm font-semibold"><ShieldCheck size={17} className="text-emerald-600" aria-hidden="true" /> 데이터 보호</p>
            <p className="mt-2 text-xs leading-5 text-slate-500">개인 위키는 계정 ID별로 분리되고, 민감정보는 저장 전에 자동 마스킹됩니다.</p>
          </section>
        </aside>

        <section className="min-w-0">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="text-lg font-semibold">개인 위키</h2>
              <p className="mt-1 text-sm text-slate-500">FUB가 나에 대해 기억하는 정보입니다.</p>
            </div>
            <button type="button" onClick={openCreate} className="flex h-10 items-center justify-center gap-2 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800">
              <Plus size={16} aria-hidden="true" /> 기억 추가
            </button>
          </div>

          <div className="mt-4 flex h-11 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 shadow-sm focus-within:border-cyan-600 focus-within:ring-2 focus-within:ring-cyan-100">
            <Search size={17} className="text-slate-400" aria-hidden="true" />
            <input value={query} onChange={(event) => setQuery(event.target.value)} className="min-w-0 flex-1 border-0 bg-transparent text-sm outline-none" placeholder="기억 검색" />
            {query && <button type="button" onClick={() => setQuery('')} className="grid size-7 place-items-center rounded text-slate-400 hover:bg-slate-100" aria-label="검색어 지우기"><X size={15} /></button>}
          </div>

          <div className="mt-3 flex gap-2 overflow-x-auto pb-1" aria-label="위키 카테고리">
            {categories.map((item) => (
              <button key={item} type="button" onClick={() => setCategory(item)} className={`shrink-0 rounded-full border px-3 py-1.5 text-xs font-semibold transition ${category === item ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-300 bg-white text-slate-500 hover:border-slate-400'}`}>
                {item === 'ALL' ? `전체 ${entries.length}` : categoryMeta(item).label}
              </button>
            ))}
          </div>

          {isLoading ? (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {Array.from({ length: 4 }, (_, index) => <div key={index} className="h-40 animate-pulse rounded-lg border border-slate-200 bg-white p-4"><div className="h-6 w-24 rounded bg-slate-100" /><div className="mt-5 h-4 rounded bg-slate-100" /><div className="mt-2 h-4 w-2/3 rounded bg-slate-100" /></div>)}
            </div>
          ) : visibleEntries.length > 0 ? (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {visibleEntries.map((entry) => {
                const meta = categoryMeta(entry.category)
                const Icon = meta.icon
                return (
                  <article key={entry.wikiEntrySq} className="group flex min-h-40 flex-col rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:border-cyan-200 hover:shadow">
                    <div className="flex items-start justify-between gap-3">
                      <span className={`inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-semibold ${meta.color}`}><Icon size={13} aria-hidden="true" /> {meta.label}</span>
                      <div className="flex gap-1 opacity-100 transition sm:opacity-0 sm:group-hover:opacity-100 sm:group-focus-within:opacity-100">
                        <button type="button" onClick={() => openEdit(entry)} className="grid size-8 place-items-center rounded-md text-slate-400 hover:bg-cyan-50 hover:text-cyan-700" aria-label="수정" title="수정"><Pencil size={15} /></button>
                        <button type="button" onClick={() => archiveEntry(entry)} className="grid size-8 place-items-center rounded-md text-slate-400 hover:bg-rose-50 hover:text-rose-600" aria-label="보관" title="보관"><Archive size={15} /></button>
                      </div>
                    </div>
                    <p className="mt-4 line-clamp-3 flex-1 text-sm font-medium leading-6 text-slate-800">{contentText(entry)}</p>
                    <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-400">
                      <span>{entry.sourceType === 'CHAT' ? '대화에서 추가' : '직접 입력'}</span>
                      <span>v{entry.version} · {formatDate(entry.modifiedAt)}</span>
                    </div>
                  </article>
                )
              })}
            </div>
          ) : (
            <div className="mt-4 rounded-lg border border-dashed border-slate-300 bg-white px-5 py-12 text-center">
              <BookOpenText size={28} className="mx-auto text-slate-300" aria-hidden="true" />
              <p className="mt-3 text-sm font-semibold text-slate-700">표시할 기억이 없어요</p>
              <p className="mt-1 text-xs text-slate-400">검색어나 카테고리를 바꾸거나 새로운 기억을 추가해 보세요.</p>
            </div>
          )}
        </section>
      </div>

      {editorEntry !== undefined && <WikiEditor entry={editorEntry} form={form} onChange={setForm} onClose={closeEditor} onSubmit={saveEntry} isSaving={isSaving} />}
    </div>
  )
}

export default MyPage
