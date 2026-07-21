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
      String providerCode,
      List<String> categories,
      ImageSources imageSources
  ) {
  }

  public record SearchResponse(
      String query,
      boolean live,
      List<String> sources,
      List<String> warnings,
      List<String> keywordSuggestions,
      List<String> categories,
      List<ProductItem> items
  ) {
  }

  public record ProductFeedItem(
      String productSq,
      String name,
      long price,
      String mallName,
      String source,
      String imageUrl,
      String productUrl,
      String providerCode,
      ImageSources imageSources,
      int rank,
      long lowestPrice30d,
      boolean historicalLow,
      String historyStatus,
      String priceBasis,
      long historySampleCount
  ) {
  }

  public record ImageSources(
      String card1x,
      String card2x,
      String detail,
      String original
  ) {
  }

  public record HomeFeedResponse(
      boolean live,
      List<String> warnings,
      boolean personalized,
      boolean hasPurchaseHistory,
      String recommendationBasis,
      List<String> interestKeywords,
      List<ProductFeedItem> hotProducts,
      List<ProductFeedItem> bestPriceDeals
  ) {
  }
}
