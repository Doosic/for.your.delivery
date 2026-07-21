package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.ProductOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductOfferRepository extends JpaRepository<ProductOfferEntity, Long> {

  Optional<ProductOfferEntity> findByProviderAndExternalProductId(String provider, String externalProductId);
}
