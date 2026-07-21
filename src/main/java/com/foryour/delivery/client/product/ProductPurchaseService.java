package com.foryour.delivery.client.product;

import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.entity.PurchaseClickEntity;
import com.foryour.delivery.domain.enums.ProductOfferStatusCode;
import com.foryour.delivery.domain.enums.PurchaseClickSourceCode;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.PurchaseClickRepository;
import com.foryour.delivery.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class ProductPurchaseService {

  private static final Set<String> SUPPORTED_PROVIDERS = Set.of("NAVER", "ELEVENST");

  private final ProductOfferRepository productOfferRepository;
  private final PurchaseClickRepository purchaseClickRepository;

  @Transactional
  public PurchaseClickResult recordClick(
      Long userSq,
      String provider,
      String providerCode,
      PurchaseClickSourceCode sourceContext
  ) {
    String normalizedProvider = normalizeProvider(provider);
    ProductOfferEntity offer = productOfferRepository
        .findByProviderAndExternalProductId(normalizedProvider, providerCode)
        .filter(found -> ProductOfferStatusCode.ACTIVE.equals(found.getStatus()))
        .orElseThrow(() -> new APIException(DATA_NOT_EXIST));

    PurchaseClickEntity click = new PurchaseClickEntity();
    click.setUserSq(userSq);
    click.setProductSq(offer.getProductSq());
    click.setOfferSq(offer.getOfferSq());
    click.setProvider(offer.getProvider());
    click.setExternalProductId(offer.getExternalProductId());
    click.setTargetUrl(offer.getProductUrl());
    click.setPriceAtClick(offer.getTotalPrice());
    click.setSourceContext(sourceContext == null ? PurchaseClickSourceCode.OTHER : sourceContext);
    click.setClickedAt(LocalDateTime.now());
    click = purchaseClickRepository.save(click);

    long totalCount = purchaseClickRepository.countByOfferSq(offer.getOfferSq());
    long userCount = userSq == null
        ? 0
        : purchaseClickRepository.countByOfferSqAndUserSq(offer.getOfferSq(), userSq);
    return new PurchaseClickResult(
        click.getPurchaseClickSq(),
        true,
        offer.getProvider(),
        offer.getExternalProductId(),
        offer.getProductUrl(),
        offer.getTotalPrice(),
        click.getSourceContext().name(),
        totalCount,
        userCount,
        click.getClickedAt()
    );
  }

  private String normalizeProvider(String provider) {
    if (!StringUtils.hasText(provider)) {
      throw new APIException(DATA_NOT_EXIST);
    }
    String normalized = provider.trim().toUpperCase(Locale.ROOT);
    if (!SUPPORTED_PROVIDERS.contains(normalized)) {
      throw new APIException(DATA_NOT_EXIST);
    }
    return normalized;
  }

  public record PurchaseClickResult(
      Long purchaseClickSq,
      boolean countedAsPurchase,
      String provider,
      String providerCode,
      String redirectUrl,
      long priceAtClick,
      String sourceContext,
      long totalPurchaseCount,
      long userPurchaseCount,
      LocalDateTime clickedAt
  ) {
  }
}
