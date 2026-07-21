import { constants } from '@/shared/libs/constants.js'

export const assertSuccess = (response, message = 'fail') => {
  if (String(response.status) !== constants.RESULT_SUCCESS) {
    throw new Error(response.msg || message)
  }

  return response
}

export const isDuplicateResult = (response) => {
  return String(response.status) === constants.RESULT_FAIL_DUPLICATE
}
