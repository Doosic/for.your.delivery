import { createAsyncThunk, createSlice } from '@reduxjs/toolkit'
import authService from '@/services/authService.js'

export const refreshAuth = createAsyncThunk('auth/refresh', async (_, { rejectWithValue }) => {
  try {
    return await authService().me()
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

const initialState = {
  user: null,
  status: 'loading',
  error: null,
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser(state, action) {
      state.user = action.payload
      state.status = 'authenticated'
      state.error = null
    },
    clearUser(state) {
      state.user = null
      state.status = 'guest'
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(refreshAuth.pending, (state) => {
        state.status = 'loading'
        state.error = null
      })
      .addCase(refreshAuth.fulfilled, (state, action) => {
        if (!action.payload?.email) {
          state.user = null
          state.status = 'guest'
          state.error = null
          return
        }

        state.user = action.payload
        state.status = 'authenticated'
        state.error = null
      })
      .addCase(refreshAuth.rejected, (state, action) => {
        state.user = null
        state.status = 'guest'
        state.error = action.payload ?? action.error.message ?? null
      })
  },
})

export const { setUser, clearUser } = authSlice.actions
export const selectAuth = (state) => state.auth
export default authSlice.reducer
