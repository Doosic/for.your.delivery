package com.foryour.delivery.client.product;

import com.foryour.delivery.common.APIDataResponse;
import com.foryour.delivery.common.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ProductController extends BaseController {

  private final ProductService productService;
  private final ProductImageService productImageService;

  @GetMapping("/wp/home/feed")
  public APIDataResponse<ProductModels.HomeFeedResponse> homeFeed() {
    return APIDataResponse.of(productService.homeFeed());
  }

  @GetMapping("/wb/home/feed")
  public APIDataResponse<ProductModels.HomeFeedResponse> personalizedHomeFeed() {
    return APIDataResponse.of(productService.homeFeed(getSessionInfo().getUserSq()));
  }

  @GetMapping("/wp/products")
  public APIDataResponse<ProductModels.SearchResponse> search(
      @RequestParam(defaultValue = "생활용품") String query,
      @RequestParam(defaultValue = "20") int size
  ) {
    return APIDataResponse.of(productService.search(query, size));
  }

  @GetMapping("/wp/products/{productSq}")
  public APIDataResponse<Map<String, ProductModels.ProductItem>> detail(@PathVariable String productSq) {
    return APIDataResponse.of(Map.of("product", productService.detail(productSq)));
  }

  @GetMapping(value = "/wp/product-images/{providerCode}", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<byte[]> productImage(
      @PathVariable String providerCode,
      @RequestParam(defaultValue = "640") int width
  ) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_JPEG)
        .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
        .body(productImageService.resizedNaverImage(providerCode, width));
  }
}
