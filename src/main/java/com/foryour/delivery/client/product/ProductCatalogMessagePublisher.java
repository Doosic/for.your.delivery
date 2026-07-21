package com.foryour.delivery.client.product;

import com.foryour.delivery.client.product.ProductModels.ProductItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCatalogMessagePublisher {

  private final RabbitTemplate rabbitTemplate;

  public void publish(ProductCatalogCollectionType type, String keyword, List<ProductItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }

    ProductCatalogCollectionMessage message =
        new ProductCatalogCollectionMessage(type, keyword, List.copyOf(items), LocalDateTime.now());
    try {
      rabbitTemplate.convertAndSend(ProductCatalogMq.EXCHANGE, ProductCatalogMq.ROUTING_KEY, message);
    } catch (AmqpException error) {
      log.warn("Product catalog message publish failed: {}", error.getMessage());
    }
  }
}
