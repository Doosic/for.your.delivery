package com.foryour.delivery.client.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCollectionScheduler {

  private final ProductService productService;

  @Scheduled(
      initialDelayString = "${delivery.product.price-collection-initial-delay-ms:60000}",
      fixedDelayString = "${delivery.product.price-collection-fixed-delay-ms:21600000}"
  )
  public void collectPrices() {
    try {
      productService.collectDefaultPriceHistory();
    } catch (Exception error) {
      log.warn("Scheduled product price collection failed: {}", error.getMessage());
    }
  }
}
