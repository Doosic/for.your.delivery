package com.foryour.delivery.client.product;

import com.foryour.delivery.common.CProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.foryour.delivery.client.product.ProductModels.ProductItem;
import static com.foryour.delivery.client.product.ProductModels.HomeFeedResponse;
import static com.foryour.delivery.client.product.ProductModels.SearchResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

  private static final List<String> KEYWORDS = List.of(
      "고양이 사료", "고양이 간식", "고양이 모래", "세탁세제", "캠핑 준비물", "생일 선물"
  );

  private final CProperties properties;
  private final Map<String, ProductItem> productCache = new ConcurrentHashMap<>();

  public SearchResponse search(String query, int requestedSize) {
    String keyword = StringUtils.hasText(query) ? query.trim() : "고양이 사료";
    int size = Math.max(1, Math.min(requestedSize, 40));

    CompletableFuture<List<ProductItem>> naverFuture = CompletableFuture
        .supplyAsync(() -> searchNaver(keyword, size))
        .completeOnTimeout(List.of(), 2, TimeUnit.SECONDS)
        .exceptionally(error -> List.of());
    CompletableFuture<List<ProductItem>> elevenFuture = CompletableFuture
        .supplyAsync(() -> searchElevenst(keyword, size))
        .completeOnTimeout(List.of(), 2, TimeUnit.SECONDS)
        .exceptionally(error -> List.of());

    CompletableFuture.allOf(naverFuture, elevenFuture).join();
    List<ProductItem> naverItems = naverFuture.join();
    List<ProductItem> elevenItems = elevenFuture.join();
    List<String> sources = new ArrayList<>();
    if (!naverItems.isEmpty()) sources.add("NAVER");
    if (!elevenItems.isEmpty()) sources.add("ELEVENST");

    LinkedHashMap<String, ProductItem> merged = new LinkedHashMap<>();
    naverItems.forEach(item -> merged.put(item.productSq(), item));
    elevenItems.forEach(item -> merged.put(item.productSq(), item));

    List<String> warnings = new ArrayList<>();
    if (naverItems.isEmpty()) warnings.add("네이버 상품 검색 결과를 불러오지 못했습니다.");
    if (elevenItems.isEmpty()) warnings.add("11번가 상품 검색 결과를 불러오지 못했습니다.");

    boolean live = !merged.isEmpty();
    List<ProductItem> items = live
        ? merged.values().stream().limit(size).toList()
        : demoProducts(keyword).stream().limit(size).toList();
    items.forEach(item -> productCache.put(item.productSq(), item));

    if (!live) {
      warnings.add("외부 상품 API가 연결되지 않아 서버 데모 데이터를 표시합니다.");
    }
    return new SearchResponse(keyword, live, sources, warnings, KEYWORDS, items);
  }

  public HomeFeedResponse homeFeed() {
    SearchResponse response = search("고양이 사료", 8);
    List<ProductItem> items = response.items() == null ? List.of() : response.items();
    List<ProductItem> hotProducts = items.stream()
        .limit(4)
        .toList();
    List<ProductItem> bestPriceDeals = items.stream()
        .sorted(Comparator.comparingLong(ProductItem::price))
        .limit(3)
        .toList();

    return new HomeFeedResponse(response.live(), response.warnings(), hotProducts, bestPriceDeals);
  }

  public ProductItem detail(String productSq) {
    ProductItem cached = productCache.get(productSq);
    if (cached != null) return cached;
    return demoProducts("추천").stream()
        .filter(item -> item.productSq().equals(productSq))
        .findFirst()
        .orElse(demoProducts("추천").getFirst());
  }

  @SuppressWarnings("unchecked")
  private List<ProductItem> searchNaver(String keyword, int size) {
    CProperties.Naver config = properties.getExternal().getNaver();
    if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getClientSecret())) {
      return List.of();
    }

    try {
      Map<String, Object> response = RestClient.create("https://openapi.naver.com")
          .get()
          .uri(builder -> builder.path("/v1/search/shop.json")
              .queryParam("query", keyword)
              .queryParam("display", size)
              .queryParam("sort", "sim")
              .queryParam("exclude", "used:cbshop")
              .build())
          .header("X-Naver-Client-Id", config.getClientId())
          .header("X-Naver-Client-Secret", config.getClientSecret())
          .retrieve()
          .body(Map.class);
      if (response == null || !(response.get("items") instanceof List<?> rawItems)) return List.of();

      List<ProductItem> items = new ArrayList<>();
      for (Object raw : rawItems) {
        if (!(raw instanceof Map<?, ?> item)) continue;
        String id = stringValue(item.get("productId"), String.valueOf(items.size() + 1));
        items.add(new ProductItem(
            "NAVER-" + id,
            cleanTitle(stringValue(item.get("title"), keyword)),
            longValue(item.get("lprice")),
            stringValue(item.get("mallName"), "네이버쇼핑"),
            "NAVER",
            stringValue(item.get("image"), ""),
            stringValue(item.get("link"), ""),
            id
        ));
      }
      return items;
    } catch (Exception error) {
      log.warn("Naver product search failed: {}", error.getMessage());
      return List.of();
    }
  }

  private List<ProductItem> searchElevenst(String keyword, int size) {
    String apiKey = properties.getExternal().getElevenst().getApiKey();
    if (!StringUtils.hasText(apiKey)) return List.of();

    try {
      String xml = RestClient.create("https://openapi.11st.co.kr")
          .get()
          .uri(builder -> builder.path("/openapi/OpenApiService.tmall")
              .queryParam("key", apiKey)
              .queryParam("apiCode", "ProductSearch")
              .queryParam("keyword", keyword)
              .queryParam("pageSize", size)
              .build())
          .retrieve()
          .body(String.class);
      if (!StringUtils.hasText(xml)) return List.of();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      NodeList nodes = factory.newDocumentBuilder()
          .parse(new InputSource(new StringReader(xml)))
          .getElementsByTagName("Product");

      List<ProductItem> items = new ArrayList<>();
      for (int index = 0; index < nodes.getLength(); index++) {
        Element product = (Element) nodes.item(index);
        String id = childText(product, "ProductCode", String.valueOf(index + 1));
        items.add(new ProductItem(
            "ELEVENST-" + id,
            childText(product, "ProductName", keyword),
            longValue(childText(product, "ProductPrice", "0")),
            childText(product, "SellerNick", "11번가"),
            "ELEVENST",
            childText(product, "ProductImage", ""),
            childText(product, "DetailPageUrl", ""),
            id
        ));
      }
      return items;
    } catch (Exception error) {
      log.warn("11st product search failed: {}", error.getMessage());
      return List.of();
    }
  }

  private List<ProductItem> demoProducts(String keyword) {
    return List.of(
        new ProductItem("DEMO-1", keyword + " 인기 상품", 28900, "통합 상품 검색", "DEMO", "", "https://search.shopping.naver.com/search/all?query=" + keyword, "SERVER_DEMO"),
        new ProductItem("DEMO-2", keyword + " 실속형", 19800, "통합 상품 검색", "DEMO", "", "https://search.11st.co.kr/Search.tmall?kwd=" + keyword, "SERVER_DEMO"),
        new ProductItem("DEMO-3", keyword + " 무료배송", 32500, "통합 상품 검색", "DEMO", "", "https://search.shopping.naver.com/search/all?query=" + keyword, "SERVER_DEMO"),
        new ProductItem("DEMO-4", keyword + " 대용량", 41900, "통합 상품 검색", "DEMO", "", "https://search.11st.co.kr/Search.tmall?kwd=" + keyword, "SERVER_DEMO")
    );
  }

  private String cleanTitle(String value) {
    return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", ""));
  }

  private String childText(Element element, String name, String fallback) {
    NodeList nodes = element.getElementsByTagName(name);
    return nodes.getLength() == 0 ? fallback : nodes.item(0).getTextContent();
  }

  private String stringValue(Object value, String fallback) {
    return value == null || value.toString().isBlank() ? fallback : value.toString();
  }

  private long longValue(Object value) {
    try {
      return value == null ? 0L : Long.parseLong(value.toString().replaceAll("[^0-9]", ""));
    } catch (NumberFormatException ignored) {
      return 0L;
    }
  }
}
