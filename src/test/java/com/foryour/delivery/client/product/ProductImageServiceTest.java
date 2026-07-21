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

import static org.assertj.core.api.Assertions.assertThat;
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
    assertThatThrownBy(() -> productImageService.resizedImage("NAVER", "unknown", 500))
        .isInstanceOf(APIException.class)
        .hasMessage("bad request");
  }

  @Test
  void returnsPlaceholderForImageHostOutsideShoppingProviders() {
    String providerCode = "image-test-" + UUID.randomUUID();
    ProductEntity product = new ProductEntity();
    product.setProductKey("NAVER:" + providerCode);
    product.setName("외부 이미지 테스트");
    product.setNormalizedName("외부 이미지 테스트");
    product.setImageUrl("https://example.com/product.jpg");
    product.setStatus(ProductStatusCode.ACTIVE);
    productRepository.saveAndFlush(product);

    byte[] image = productImageService.resizedImage("NAVER", providerCode, 320);

    assertThat(image).isNotEmpty();
    assertThat(image[0]).isEqualTo((byte) 0xFF);
    assertThat(image[1]).isEqualTo((byte) 0xD8);
  }
}
