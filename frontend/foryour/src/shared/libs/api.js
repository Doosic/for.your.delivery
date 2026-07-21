import axios from 'axios'
import dayjs from 'dayjs'

const api = axios.create({
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
})

api.defaults.headers.common.AJAX = 'true'

api.interceptors.request.use(
  (config) => {
    let url = config.url

    let cp = localStorage.getItem('delivery-contextpath')
    if (cp == null) {
      cp = '/delivery'
    }
    url = url.replace('/delivery', cp)

    const timeStamp = dayjs().format('YYYYMMDDHHmmssSSS')
    if (url.indexOf('?') > 0) {
      url += `&t=${timeStamp}`
    } else {
      url += `?t=${timeStamp}`
    }

    config.url = url
    return config
  },
  (error) => {
    return Promise.reject(error)
  },
)

api.interceptors.response.use(
  (response) => {
    if (response?.config?.responseType !== undefined && response?.config?.responseType !== 'blob') {
      if (
        response.headers['content-type']?.indexOf('application/json') !== undefined &&
        response.headers['content-type']?.indexOf('application/json') === -1
      ) {
        return response
      }
    }

    const data = response.data
    if (data && typeof data === 'object') {
      return Promise.resolve({
        ...data,
        status: data.status ?? data.statusCode,
        msg: data.msg ?? data.message,
        body: data.body ?? data.data,
      })
    }

    return Promise.resolve(data)
  },
  (error) => {
    if (error.status === 401 || error.response?.status === 401) {
      alert('로그인 세션이 종료되었습니다.')
      const cp = localStorage.getItem('delivery-contextpath') || ''
      location.href = `${cp}/login`
      return Promise.reject(error)
    }

    const data = error.response?.data
    if (data && typeof data === 'object') {
      return Promise.reject({
        ...error,
        message: data.message ?? data.msg ?? error.message,
      })
    }

    return Promise.reject(error)
  },
)

api.GET = (url, params) => {
  return api.get(url, { params })
}

api.POST = (url, formData, config = {}) => {
  return api.post(url, formData, config)
}

export default api
