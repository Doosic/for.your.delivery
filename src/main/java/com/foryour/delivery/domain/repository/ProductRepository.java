package com.foryour.delivery.domain.repository;

import com.foryour.delivery.domain.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

  Optional<ProductEntity> findByProductKey(String productKey);
}
