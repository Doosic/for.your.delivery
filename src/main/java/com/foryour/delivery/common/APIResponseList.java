package com.foryour.delivery.common;

import com.foryour.delivery.domain.dto.PagenationDTO;
import lombok.Data;

import java.util.List;

@Data
public class APIResponseList<T> extends PagenationDTO {

  private List<T> itemList;
}
