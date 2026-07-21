package com.foryour.delivery.domain.dto;

import lombok.Data;

@Data
public class PagenationDTO {

  private final static Integer DEFAULT_PAGE_ROW_COUNT = 10;
  private final static Integer DEFAULT_PAGE_NUM = 1;
  private final static Long DEFAULT_PAGE_TOTAL_COUNT = 0L;

  private Integer pageNum = DEFAULT_PAGE_NUM;
  private Integer pageRowCount = DEFAULT_PAGE_ROW_COUNT;
  private Long itemTotalCnt = DEFAULT_PAGE_TOTAL_COUNT;

  public Integer getPageNum() {
    if(pageNum == null || pageNum == 0){
      setPageNum(1);
    }
    return pageNum;
  }

  public Integer getPageRowCount() {
    if(pageRowCount == null || pageRowCount == 0){
      setPageRowCount(10);
    }
    return pageRowCount;
  }
}
