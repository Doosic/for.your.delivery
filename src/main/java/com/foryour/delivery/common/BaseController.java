package com.foryour.delivery.common;

import com.foryour.delivery.client.user.bean.UserResponseVO;
import com.foryour.delivery.exception.AccountException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import static com.foryour.delivery.domain.enums.ErrorCode.UNAUTHORIZED_FAIL;


public abstract class BaseController {

  protected UserResponseVO getSessionInfo() throws AccountException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if(authentication.getPrincipal().equals("anonymousUser")){
      throw new AccountException(UNAUTHORIZED_FAIL);
    }

    UserResponseVO userInfo = (UserResponseVO) authentication.getDetails();
    return userInfo;
  }

  protected byte[] compressData(String data) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
        gzipOutputStream.write(data.getBytes());
      }
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      e.printStackTrace();
      return new byte[0];
    }
  }

  protected String convertObjectToJson(Object object) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(object);
  }
}
