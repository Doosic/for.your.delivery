package com.foryour.delivery.client.app;

import com.foryour.delivery.common.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AppController {

  @RequestMapping(value = {"/login", "/app/**"})
  public ModelAndView app(HttpServletRequest request) {
    ModelAndView mav = new ModelAndView();
    mav.addObject("contextPath", request.getContextPath());
    mav.addObject("timestamp", Utils.getDateFormatString(LocalDateTime.now(), "yyyyMMddHHmmss"));
    mav.setViewName("app/app");
    return mav;
  }
}
