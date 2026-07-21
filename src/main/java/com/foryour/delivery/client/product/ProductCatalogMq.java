package com.foryour.delivery.client.product;

public final class ProductCatalogMq {

  public static final String EXCHANGE = "foryour.product.catalog.exchange";
  public static final String QUEUE = "foryour.product.catalog.collect";
  public static final String ROUTING_KEY = "product.catalog.collect";

  private ProductCatalogMq() {
  }
}
