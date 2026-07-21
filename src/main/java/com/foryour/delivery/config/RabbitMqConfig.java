package com.foryour.delivery.config;

import com.foryour.delivery.client.product.ProductCatalogMq;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

  @Bean
  public DirectExchange productCatalogExchange() {
    return new DirectExchange(ProductCatalogMq.EXCHANGE);
  }

  @Bean
  public Queue productCatalogQueue() {
    return new Queue(ProductCatalogMq.QUEUE, true);
  }

  @Bean
  public Binding productCatalogBinding(
      Queue productCatalogQueue,
      DirectExchange productCatalogExchange
  ) {
    return BindingBuilder
        .bind(productCatalogQueue)
        .to(productCatalogExchange)
        .with(ProductCatalogMq.ROUTING_KEY);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
