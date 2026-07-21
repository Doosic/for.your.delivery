package com.foryour.delivery.client.product;

import com.foryour.delivery.common.APIDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @GetMapping("/wp/home/feed")
  public APIDataResponse<ProductModels.HomeFeedResponse> homeFeed() {
    return APIDataResponse.of(productService.homeFeed());
  }

  @GetMapping("/wp/products")
  public APIDataResponse<ProductModels.SearchResponse> search(
      @RequestParam(defaultValue = "고양이 사료") String query,
      @RequestParam(defaultValue = "20") int size
  ) {
    return APIDataResponse.of(productService.search(query, size));
  }

  @GetMapping("/wp/products/{productSq}")
  public APIDataResponse<Map<String, ProductModels.ProductItem>> detail(@PathVariable String productSq) {
    return APIDataResponse.of(Map.of("product", productService.detail(productSq)));
  }
}
