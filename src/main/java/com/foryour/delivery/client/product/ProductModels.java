package com.foryour.delivery.client.product;

import java.util.List;

public final class ProductModels {

  private ProductModels() {
  }

  public record ProductItem(
      String productSq,
      String name,
      long price,
      String mallName,
      String source,
      String imageUrl,
      String productUrl,
      String providerCode
  ) {
  }

  public record SearchResponse(
      String query,
      boolean live,
      List<String> sources,
      List<String> warnings,
      List<String> keywordSuggestions,
      List<ProductItem> items
  ) {
  }

  public record HomeFeedResponse(
      boolean live,
      List<String> warnings,
      List<ProductItem> hotProducts,
      List<ProductItem> bestPriceDeals
  ) {
  }
}
