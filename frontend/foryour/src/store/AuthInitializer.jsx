import { useEffect } from 'react'
import { useDispatch } from 'react-redux'
import { refreshAuth } from '@/store/authSlice.js'

function AuthInitializer({ children }) {
  const dispatch = useDispatch()

  useEffect(() => {
    dispatch(refreshAuth())
  }, [dispatch])

  return children
}

export default AuthInitializer
