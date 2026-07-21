package com.foryour.delivery.client.product;

import com.foryour.delivery.domain.entity.ProductEntity;
import com.foryour.delivery.domain.enums.ProductStatusCode;
import com.foryour.delivery.domain.repository.ProductRepository;
import com.foryour.delivery.exception.APIException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductImageServiceTest {

  @Autowired
  private ProductImageService productImageService;

  @Autowired
  private ProductRepository productRepository;

  @Test
  void rejectsUnsupportedImageWidth() {
    assertThatThrownBy(() -> productImageService.resizedNaverImage("unknown", 500))
        .isInstanceOf(APIException.class)
        .hasMessage("bad request");
  }

  @Test
  void rejectsImageHostOutsideNaverShopping() {
    String providerCode = "image-test-" + UUID.randomUUID();
    ProductEntity product = new ProductEntity();
    product.setProductKey("NAVER:" + providerCode);
    product.setName("외부 이미지 테스트");
    product.setNormalizedName("외부 이미지 테스트");
    product.setImageUrl("https://example.com/product.jpg");
    product.setStatus(ProductStatusCode.ACTIVE);
    productRepository.saveAndFlush(product);

    assertThatThrownBy(() -> productImageService.resizedNaverImage(providerCode, 320))
        .isInstanceOf(APIException.class)
        .hasMessage("bad request");
  }
}
