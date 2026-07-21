import { useCallback, useEffect, useState } from 'react'

const STORAGE_KEY = 'fub-user'
const LEGACY_STORAGE_KEY = 'prebuy-user'
const AUTH_EVENT = 'fub-auth-changed'

const readUser = () => {
  const raw = localStorage.getItem(STORAGE_KEY) ?? localStorage.getItem(LEGACY_STORAGE_KEY)
  return raw ? JSON.parse(raw) : null
}

/**
 * 로그인 표시용 세션 훅. 실제 연동 시 GET /wp/user/me 응답으로 초기화한다.
 * 실제 인증은 추후 authService(JWT 쿠키)와 연결하고, 여기서는 화면 상태만 관리한다.
 * 로그인은 필수가 아니며, 모든 화면은 게스트로도 접근 가능하다.
 */
export const useAuth = () => {
  const [user, setUser] = useState(readUser)

  useEffect(() => {
    const sync = () => setUser(readUser())
    window.addEventListener(AUTH_EVENT, sync)
    return () => window.removeEventListener(AUTH_EVENT, sync)
  }, [])

  const saveUser = useCallback((userInfo) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(userInfo))
    window.dispatchEvent(new Event(AUTH_EVENT))
  }, [])

  const clearUser = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(LEGACY_STORAGE_KEY)
    window.dispatchEvent(new Event(AUTH_EVENT))
  }, [])

  return {
    user,
    isLoggedIn: user != null,
    saveUser,
    clearUser,
  }
}
