package com.foryour.delivery.client.product;

import com.foryour.delivery.client.product.ProductModels.ImageSources;
import com.foryour.delivery.client.product.ProductModels.ProductItem;
import com.foryour.delivery.client.product.ProductPurchaseService.PurchaseClickResult;
import com.foryour.delivery.domain.entity.UserEntity;
import com.foryour.delivery.domain.enums.PurchaseClickSourceCode;
import com.foryour.delivery.domain.enums.UserStatusCode;
import com.foryour.delivery.domain.repository.PurchaseClickRepository;
import com.foryour.delivery.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductPurchaseServiceTest {

  @Autowired
  private ProductCatalogService productCatalogService;

  @Autowired
  private ProductPurchaseService productPurchaseService;

  @Autowired
  private PurchaseClickRepository purchaseClickRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void countsGuestAndMemberClicksUsingPersistedOfferUrl() {
    long clickCount = purchaseClickRepository.count();
    String providerCode = "purchase-test-" + UUID.randomUUID();
    String productUrl = "https://www.11st.co.kr/products/" + providerCode;
    productCatalogService.registerOffers(List.of(elevenstItem(providerCode, productUrl)));

    PurchaseClickResult guest = productPurchaseService.recordClick(
        null, "elevenst", providerCode, PurchaseClickSourceCode.SEARCH);
    UserEntity user = createUser();
    PurchaseClickResult member = productPurchaseService.recordClick(
        user.getUserSq(), "ELEVENST", providerCode, PurchaseClickSourceCode.AGENT_CHAT);

    assertThat(guest.countedAsPurchase()).isTrue();
    assertThat(guest.redirectUrl()).isEqualTo(productUrl);
    assertThat(guest.priceAtClick()).isEqualTo(19_900L);
    assertThat(guest.totalPurchaseCount()).isEqualTo(1);
    assertThat(guest.userPurchaseCount()).isZero();

    assertThat(member.totalPurchaseCount()).isEqualTo(2);
    assertThat(member.userPurchaseCount()).isEqualTo(1);
    assertThat(member.sourceContext()).isEqualTo("AGENT_CHAT");
    assertThat(purchaseClickRepository.count()).isEqualTo(clickCount + 2);
  }

  private ProductItem elevenstItem(String providerCode, String productUrl) {
    String image = "https://cdn.011st.com/product.jpg";
    return new ProductItem(
        "ELEVENST-" + providerCode,
        "테스트 생필품",
        19_900L,
        "테스트 판매자",
        "ELEVENST",
        image,
        productUrl,
        providerCode,
        List.of(),
        new ImageSources(image, image, image, image)
    );
  }

  private UserEntity createUser() {
    UserEntity user = new UserEntity();
    user.setEmail("purchase-test-" + UUID.randomUUID() + "@example.com");
    user.setName("purchase test");
    user.setPassword("test-password");
    user.setStatus(UserStatusCode.USE);
    return userRepository.saveAndFlush(user);
  }
}
