import { LogIn, Settings, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import ApiNameChip from '@/components/ApiNameChip.jsx'
import { API_ENDPOINTS } from '@/services/apiBlueprint.js'
import { importService } from '@/services/importService.js'
import { importMock } from '@/services/mockData.js'
import { useAlert } from '@/shared/hooks/useAlert.jsx'
import { useAuth } from '@/shared/hooks/useAuth.jsx'

function ImportListPage() {
  const alert = useAlert()
  const { isLoggedIn } = useAuth()
  const { openLogin } = useOutletContext()
  const [items, setItems] = useState(importMock.items)
  const [checked, setChecked] = useState(() => new Set())
  const [isLive, setIsLive] = useState(false)

  useEffect(() => {
    importService.getItems().then((response) => {
      setItems(response.items)
      setIsLive(Boolean(response.live))
    })
  }, [])

  const toggle = (itemSq) => {
    setChecked((previous) => {
      const next = new Set(previous)
      if (next.has(itemSq)) next.delete(itemSq)
      else next.add(itemSq)
      return next
    })
  }

  const handleDelete = (itemSq) => {
    setItems((previous) => previous.filter((item) => item.importedItemSq !== itemSq))
    setChecked((previous) => {
      const next = new Set(previous)
      next.delete(itemSq)
      return next
    })
  }

  const handleCommit = async () => {
    const selectedIds = [...checked]
    const response = await importService.commit(selectedIds)
    setItems((previous) => previous.filter((item) => !checked.has(item.importedItemSq)))
    setChecked(new Set())
    await alert.alertSuccess('알림', `${response.committedCount ?? selectedIds.length}건이 재고에 반영되었습니다.`)
  }

  return (
    <div className="space-y-5">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">구매목록 가져오기</h1>
          <div className="mt-2 flex flex-wrap gap-2">
            <ApiNameChip>{API_ENDPOINTS.importsItems}</ApiNameChip>
            <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${isLive ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
              {isLive ? '실제 데이터' : '목업 데이터'}
            </span>
          </div>
        </div>
        <Link
          to="/app/imports/connections"
          className="flex h-9 items-center gap-1.5 rounded-md border border-slate-300 px-3 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
        >
          <Settings size={14} aria-hidden="true" /> 소스 관리
        </Link>
      </header>

      {!isLoggedIn && (
        <section className="rounded-lg border border-cyan-200 bg-white p-5 shadow-sm">
          <p className="flex items-center gap-2 text-sm font-semibold text-cyan-800">
            <LogIn size={16} aria-hidden="true" /> 로그인이 필요한 화면
          </p>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            네이버, 쿠팡, Gmail 구매내역과 구글 캘린더 일정은 로그인 후 연결할 수 있어요. 지금은 화면 흐름 확인을 위한 예시 데이터가 표시됩니다.
          </p>
          <button
            type="button"
            onClick={openLogin}
            className="mt-4 h-10 rounded-md bg-cyan-700 px-4 text-sm font-semibold text-white transition hover:bg-cyan-800"
          >
            로그인하고 가져오기
          </button>
        </section>
      )}

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-4">
          <h2 className="text-base font-semibold">가져온 구매목록 {items.length}건</h2>
        </div>
        {items.length === 0 ? (
          <p className="p-10 text-center text-sm text-slate-400">모든 항목이 재고에 반영됐어요</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {items.map((item) => (
              <li key={item.importedItemSq} className="flex items-center gap-3 p-4 transition hover:bg-slate-50">
                <label className="flex min-w-0 flex-1 cursor-pointer items-center gap-4">
                  <input
                    type="checkbox"
                    className="h-4 w-4 accent-cyan-700"
                    checked={checked.has(item.importedItemSq)}
                    onChange={() => toggle(item.importedItemSq)}
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">{item.name}</p>
                    <p className="text-xs text-slate-500">
                      {item.quantity}개 · {item.price.toLocaleString()}원 · {item.purchasedAt}
                    </p>
                  </div>
                </label>
                <button
                  type="button"
                  onClick={() => handleDelete(item.importedItemSq)}
                  className="flex h-9 shrink-0 items-center gap-1.5 rounded-md border border-slate-300 px-3 text-sm font-medium text-slate-600 transition hover:border-rose-200 hover:bg-rose-50 hover:text-rose-700"
                  aria-label={`${item.name} 삭제`}
                >
                  <Trash2 size={14} aria-hidden="true" />
                  삭제
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <div className="rounded-lg bg-cyan-50 p-4 text-sm leading-6 text-slate-600">
        <span className="font-semibold text-cyan-800">요약 Agent 자동 보정 · </span>
        품목명 정규화, 카테고리·예상 소진주기 부여 완료. 유통기한이 필요한 항목은 저장 후 알려드릴게요.
      </div>

      <button
        type="button"
        onClick={handleCommit}
        disabled={!isLoggedIn || checked.size === 0}
        className="h-12 w-full rounded-md bg-cyan-700 text-sm font-semibold text-white transition hover:bg-cyan-800 disabled:cursor-not-allowed disabled:opacity-40 sm:w-auto sm:px-8"
      >
        선택 {checked.size}건 재고에 반영
      </button>
    </div>
  )
}

export default ImportListPage
