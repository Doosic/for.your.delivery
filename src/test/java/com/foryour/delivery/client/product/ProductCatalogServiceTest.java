package com.foryour.delivery.client.product;

import com.foryour.delivery.client.product.ProductModels.ImageSources;
import com.foryour.delivery.client.product.ProductModels.ProductFeedItem;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.entity.ProductPriceHistoryEntity;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.ProductPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductCatalogServiceTest {

  @Autowired
  private ProductCatalogService productCatalogService;

  @Autowired
  private ProductOfferRepository productOfferRepository;

  @Autowired
  private ProductPriceHistoryRepository productPriceHistoryRepository;

  @Test
  void startsWithNaverLowestPriceAndMarksHistoricalLowAfterTwoSamples() {
    String providerCode = "catalog-test-" + UUID.randomUUID();
    ProductItem item = naverItem(providerCode, 10_000L);

    ProductFeedItem first = productCatalogService.collectAndEnrich(List.of(item)).getFirst();

    assertThat(first.priceBasis()).isEqualTo("NAVER_LPRICE");
    assertThat(first.rank()).isEqualTo(1);
    assertThat(first.historyStatus()).isEqualTo("COLLECTING");
    assertThat(first.historySampleCount()).isEqualTo(1);
    assertThat(first.historicalLow()).isFalse();

    ProductOfferEntity offer = productOfferRepository
        .findByProviderAndExternalProductId("NAVER", providerCode)
        .orElseThrow();
    ProductPriceHistoryEntity olderPrice = new ProductPriceHistoryEntity();
    olderPrice.setOfferSq(offer.getOfferSq());
    olderPrice.setPrice(12_000L);
    olderPrice.setShippingFee(0L);
    olderPrice.setTotalPrice(12_000L);
    olderPrice.setCollectedAt(LocalDateTime.now().minusDays(1));
    productPriceHistoryRepository.saveAndFlush(olderPrice);

    ProductFeedItem second = productCatalogService.collectAndEnrich(List.of(item)).getFirst();

    assertThat(second.historyStatus()).isEqualTo("READY");
    assertThat(second.historySampleCount()).isEqualTo(2);
    assertThat(second.lowestPrice30d()).isEqualTo(10_000L);
    assertThat(second.historicalLow()).isTrue();
  }

  private ProductItem naverItem(String providerCode, long price) {
    String original = "https://shopping-phinf.pstatic.net/main_1234567/1234567.1.jpg";
    return new ProductItem(
        "NAVER:" + providerCode,
        "테스트 생활용품",
        price,
        "테스트몰",
        "NAVER",
        original,
        "https://search.shopping.naver.com/catalog/" + providerCode,
        providerCode,
        List.of("생활/건강", "생활용품"),
        new ImageSources("/card-1x", "/card-2x", "/detail", original)
    );
  }
}
