import Rsa from '@/shared/libs/rsa/rsa.js'
import api from '@/shared/libs/api.js'
import { assertSuccess, assertSuccessBody } from '@/shared/libs/api-result.js'

export const authService = () => {
  const getCryptoPublicKey = async () => {
    const response = await api.POST('/delivery/wp/user/crypto-public-key')
    return assertSuccess(response, '로그인에 실패했습니다.')
  }

  const login = async (email, password) => {
    const response = await getCryptoPublicKey()
    const rsa = new Rsa()
    rsa.setPublic(response.body.publicKeyModulus, response.body.publicKeyExponent)

    const loginResponse = await api.POST('/delivery/wp/user/login', {
      email: rsa.encrypt(email),
      password: rsa.encrypt(password),
    })

    return assertSuccessBody(loginResponse, loginResponse.msg || 'fail')
  }

  const signup = async ({ name, email, password }) => {
    const response = await api.POST('/delivery/wp/user/signup', {
      name,
      email,
      password,
    })

    return assertSuccessBody(response, response.msg || 'fail')
  }

  const logout = async () => {
    const response = await api.GET('/delivery/wb/user/logout', undefined, {
      skipUnauthorizedRedirect: true,
    })
    return assertSuccess(response, response.msg || 'fail')
  }

  const me = async () => {
    const response = await api.GET('/delivery/wb/user/me', undefined, {
      skipUnauthorizedRedirect: true,
    })
    return assertSuccessBody(response, response.msg || '사용자 정보를 불러오지 못했습니다.')
  }

  return {
    login,
    signup,
    logout,
    me,
    getCryptoPublicKey,
  }
}

export default authService
