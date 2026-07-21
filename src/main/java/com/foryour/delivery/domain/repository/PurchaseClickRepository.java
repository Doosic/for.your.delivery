package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.PurchaseClickEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseClickRepository extends JpaRepository<PurchaseClickEntity, Long> {

  long countByOfferSq(Long offerSq);

  long countByOfferSqAndUserSq(Long offerSq, Long userSq);
}
