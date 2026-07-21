import { useCallback } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { clearUser, selectAuth, setUser } from '@/store/authSlice.js'

export const useAuth = () => {
  const dispatch = useDispatch()
  const { user, status } = useSelector(selectAuth)

  const saveUser = useCallback((userInfo) => {
    dispatch(setUser(userInfo))
  }, [dispatch])

  const removeUser = useCallback(() => {
    dispatch(clearUser())
  }, [dispatch])

  return {
    user,
    status,
    isLoggedIn: user != null,
    saveUser,
    clearUser: removeUser,
  }
}
