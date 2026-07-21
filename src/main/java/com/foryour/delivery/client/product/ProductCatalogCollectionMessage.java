package com.foryour.delivery.client.product;

import com.foryour.delivery.client.product.ProductModels.ProductItem;

import java.time.LocalDateTime;
import java.util.List;

public record ProductCatalogCollectionMessage(
    ProductCatalogCollectionType type,
    String keyword,
    List<ProductItem> items,
    LocalDateTime requestedAt
) {
}
