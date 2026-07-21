package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.PurchaseClickEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseClickRepository extends JpaRepository<PurchaseClickEntity, Long> {

  long countByOfferSq(Long offerSq);

  long countByOfferSqAndUserSq(Long offerSq, Long userSq);

  long countByProviderAndExternalProductId(String provider, String externalProductId);

  List<PurchaseClickEntity> findAllByUserSqOrderByClickedAtDesc(Long userSq);
}
