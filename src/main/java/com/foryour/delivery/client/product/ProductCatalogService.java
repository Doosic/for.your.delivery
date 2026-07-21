package com.foryour.delivery.client.product;

import com.foryour.delivery.client.product.ProductModels.ProductFeedItem;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.domain.entity.ProductEntity;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.entity.ProductPriceHistoryEntity;
import com.foryour.delivery.domain.enums.ProductOfferStatusCode;
import com.foryour.delivery.domain.enums.ProductStatusCode;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.ProductPriceHistoryRepository;
import com.foryour.delivery.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

  private static final int HISTORY_DAYS = 30;
  private static final int MAX_COLLECTED_RANK = 10;
  private static final int MIN_HISTORY_SAMPLES = 2;
  private static final int SAMPLE_INTERVAL_HOURS = 6;

  private final ProductRepository productRepository;
  private final ProductOfferRepository productOfferRepository;
  private final ProductPriceHistoryRepository productPriceHistoryRepository;

  @Transactional
  public synchronized List<ProductFeedItem> collectAndEnrich(List<ProductItem> items) {
    LocalDateTime collectedAt = LocalDateTime.now();
    List<ProductFeedItem> result = new ArrayList<>();

    for (int index = 0; index < Math.min(items.size(), MAX_COLLECTED_RANK); index++) {
      ProductItem item = items.get(index);
      if (!"NAVER".equals(item.source()) || item.price() <= 0) {
        continue;
      }

      ProductEntity product = upsertProduct(item);
      ProductOfferEntity offer = upsertOffer(product, item, index + 1, collectedAt);
      collectPriceIfNeeded(offer, item, collectedAt);
      result.add(toFeedItem(item, offer, index + 1, collectedAt.minusDays(HISTORY_DAYS)));
    }
    return result;
  }

  @Transactional
  public synchronized void registerOffers(List<ProductItem> items) {
    LocalDateTime collectedAt = LocalDateTime.now();
    for (ProductItem item : items) {
      if (!isSupportedProvider(item.source()) || item.price() <= 0) {
        continue;
      }
      ProductEntity product = upsertProduct(item);
      upsertOffer(product, item, 0, collectedAt);
    }
  }

  private ProductEntity upsertProduct(ProductItem item) {
    String productKey = item.source() + ":" + item.providerCode();
    Optional<ProductEntity> productOptional = productRepository.findByProductKey(productKey);
    ProductEntity product;
    if (productOptional.isPresent()) {
      product = productOptional.get();
    } else {
      product = new ProductEntity();
    }
    product.setProductKey(productKey);
    product.setName(item.name());
    product.setNormalizedName(normalizeName(item.name()));
    product.setBrand(item.brand());
    product.setMaker(item.maker());
    product.setCategoryPath(String.join(" > ", item.categories()));
    product.setImageUrl(item.imageSources() == null ? item.imageUrl() : item.imageSources().original());
    product.setStatus(ProductStatusCode.ACTIVE);
    return productRepository.save(product);
  }

  private ProductOfferEntity upsertOffer(
      ProductEntity product,
      ProductItem item,
      int rank,
      LocalDateTime collectedAt
  ) {
    Optional<ProductOfferEntity> offerOptional =
        productOfferRepository.findByProviderAndExternalProductId(item.source(), item.providerCode());
    ProductOfferEntity offer;
    if (offerOptional.isPresent()) {
      offer = offerOptional.get();
    } else {
      offer = new ProductOfferEntity();
    }
    offer.setProductSq(product.getProductSq());
    offer.setProvider(item.source());
    offer.setExternalProductId(item.providerCode());
    offer.setMallName(item.mallName());
    offer.setProductUrl(item.productUrl());
    offer.setPrice(item.price());
    offer.setShippingFee(0L);
    offer.setTotalPrice(item.price());
    offer.setSearchRank(rank);
    offer.setStatus(ProductOfferStatusCode.ACTIVE);
    offer.setLastCollectedAt(collectedAt);
    return productOfferRepository.save(offer);
  }

  private void collectPriceIfNeeded(
      ProductOfferEntity offer,
      ProductItem item,
      LocalDateTime collectedAt
  ) {
    ProductPriceHistoryEntity latest = productPriceHistoryRepository
        .findFirstByOfferSqOrderByCollectedAtDesc(offer.getOfferSq())
        .orElse(null);
    boolean priceChanged = latest != null && latest.getTotalPrice() != item.price();
    boolean intervalPassed = latest == null
        || latest.getCollectedAt().isBefore(collectedAt.minusHours(SAMPLE_INTERVAL_HOURS));
    if (!priceChanged && !intervalPassed) {
      return;
    }

    ProductPriceHistoryEntity history = new ProductPriceHistoryEntity();
    history.setOfferSq(offer.getOfferSq());
    history.setPrice(item.price());
    history.setShippingFee(0L);
    history.setTotalPrice(item.price());
    history.setCollectedAt(collectedAt);
    productPriceHistoryRepository.save(history);
  }

  private ProductFeedItem toFeedItem(
      ProductItem item,
      ProductOfferEntity offer,
      int rank,
      LocalDateTime collectedAfter
  ) {
    long sampleCount = productPriceHistoryRepository
        .countByOfferSqAndCollectedAtGreaterThanEqual(offer.getOfferSq(), collectedAfter);
    Long lowestPrice30d = productPriceHistoryRepository
        .findLowestTotalPrice(offer.getOfferSq(), collectedAfter);
    boolean historyReady = sampleCount >= MIN_HISTORY_SAMPLES;
    boolean historicalLow = historyReady && lowestPrice30d != null && item.price() <= lowestPrice30d;

    return new ProductFeedItem(
        item.productSq(),
        item.name(),
        item.price(),
        item.mallName(),
        item.source(),
        item.imageUrl(),
        item.productUrl(),
        item.providerCode(),
        item.imageSources(),
        rank,
        lowestPrice30d == null ? item.price() : lowestPrice30d,
        historicalLow,
        historyReady ? "READY" : "COLLECTING",
        "NAVER_LPRICE",
        sampleCount
    );
  }

  private String normalizeName(String value) {
    return value.toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private boolean isSupportedProvider(String provider) {
    return "NAVER".equals(provider) || "ELEVENST".equals(provider);
  }
}
