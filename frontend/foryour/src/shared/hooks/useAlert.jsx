/* eslint-disable react-refresh/only-export-components */
import React from 'react'
import Swal from 'sweetalert2'
import withReactContent from 'sweetalert2-react-content'

const MySwal = withReactContent(Swal)

const alertBase = {
  fire: MySwal.fire,
  alertShow: (title = null, text = null) => {
    MySwal.fire({
      title,
      text,
      icon: 'info',
      customClass: {
        container: 'swal-z-top',
      },
    })
  },
  alertSuccess: (title = null, text = null) => {
    MySwal.fire({
      title,
      text,
      icon: 'success',
      customClass: {
        container: 'swal-z-top',
      },
    })
  },
  alertWarning: (title = null, text = null) => {
    MySwal.fire({
      title,
      text,
      icon: 'warning',
      customClass: {
        container: 'swal-z-top',
      },
    })
  },
  alertWarningHtml: (title = null, html = null) => {
    MySwal.fire({
      title,
      html,
      icon: 'warning',
      customClass: {
        container: 'swal-z-top',
      },
    })
  },
  alertError: (title = null, text = null) => {
    MySwal.fire({
      title,
      text,
      icon: 'error',
      customClass: {
        container: 'swal-z-top',
      },
    })
  },
  alertConfirm: async (title = null, text = null, confirmText = 'OK', cancelText = 'Cancel') => {
    const result = await MySwal.fire({
      icon: 'question',
      title,
      text,
      showCloseButton: true,
      showCancelButton: true,
      cancelButtonText: cancelText,
      confirmButtonText: confirmText,
      customClass: {
        container: 'swal-z-top',
      },
    })

    return result.isConfirmed
  },
  alertConfirmHtml: async (title = null, html = null, confirmText = 'OK', cancelText = 'Cancel') => {
    const result = await MySwal.fire({
      icon: 'question',
      title,
      html,
      showCloseButton: true,
      showCancelButton: true,
      cancelButtonText: cancelText,
      confirmButtonText: confirmText,
      customClass: {
        container: 'swal-z-top',
      },
    })

    return result.isConfirmed
  },
}

export const SwalContext = React.createContext(alertBase)

export const SwalProvider = ({ children }) => (
  <SwalContext.Provider value={alertBase}>{children}</SwalContext.Provider>
)

export const useAlert = () => React.useContext(SwalContext)
