package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.ProductPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistoryEntity, Long> {

  Optional<ProductPriceHistoryEntity> findFirstByOfferSqOrderByCollectedAtDesc(Long offerSq);

  long countByOfferSqAndCollectedAtGreaterThanEqual(Long offerSq, LocalDateTime collectedAfter);

  @Query("""
      select min(history.totalPrice)
      from ProductPriceHistoryEntity history
      where history.offerSq = :offerSq
        and history.collectedAt >= :collectedAfter
      """)
  Long findLowestTotalPrice(
      @Param("offerSq") Long offerSq,
      @Param("collectedAfter") LocalDateTime collectedAfter
  );
}
