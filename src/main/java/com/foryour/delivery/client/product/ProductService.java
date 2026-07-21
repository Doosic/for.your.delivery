package com.foryour.delivery.client.product;

import com.foryour.delivery.common.CProperties;
import com.foryour.delivery.domain.entity.ProductEntity;
import com.foryour.delivery.domain.entity.PurchaseClickEntity;
import com.foryour.delivery.domain.entity.WikiEntryEntity;
import com.foryour.delivery.domain.enums.WikiEntryStatusCode;
import com.foryour.delivery.domain.repository.PurchaseClickRepository;
import com.foryour.delivery.domain.repository.ProductRepository;
import com.foryour.delivery.domain.repository.WikiEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.foryour.delivery.client.product.ProductModels.ProductItem;
import static com.foryour.delivery.client.product.ProductModels.ProductFeedItem;
import static com.foryour.delivery.client.product.ProductModels.ImageSources;
import static com.foryour.delivery.client.product.ProductModels.HomeFeedResponse;
import static com.foryour.delivery.client.product.ProductModels.SearchResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

  private static final List<String> KEYWORDS = List.of(
      "생활용품", "식품", "반려동물", "스포츠/레저", "디지털", "가구/인테리어"
  );

  private final CProperties properties;
  private final ProductCatalogMessagePublisher productCatalogMessagePublisher;
  private final WikiEntryRepository wikiEntryRepository;
  private final PurchaseClickRepository purchaseClickRepository;
  private final ProductRepository productRepository;
  private final ProductImageService productImageService;
  private final Map<String, ProductItem> productCache = new ConcurrentHashMap<>();

  public SearchResponse search(String query, int requestedSize) {
    return search(query, requestedSize, "LOW_PRICE");
  }

  public SearchResponse search(String query, int requestedSize, String requestedSort) {
    return searchInternal(null, query, requestedSize, requestedSort);
  }

  public SearchResponse searchForUser(
      Long userSq,
      String query,
      int requestedSize,
      String requestedSort
  ) {
    String resolvedQuery = StringUtils.hasText(query)
        ? query.trim()
        : recommendationProfile(userSq).keywords().getFirst();
    return searchInternal(userSq, resolvedQuery, requestedSize, requestedSort);
  }

  private SearchResponse searchInternal(
      Long userSq,
      String query,
      int requestedSize,
      String requestedSort
  ) {
    String keyword = StringUtils.hasText(query) ? query.trim() : "생활용품";
    int size = Math.max(1, Math.min(requestedSize, 40));
    SearchSort sort = SearchSort.from(requestedSort);

    List<ProductItem> naverItems = searchNaver(keyword, size);
    List<ProductItem> elevenItems = searchElevenst(keyword, size);
    collectSafely(keyword, naverItems);
    registerSafely(keyword, elevenItems);
    List<String> sources = new ArrayList<>();
    if (!naverItems.isEmpty()) sources.add("NAVER");
    if (!elevenItems.isEmpty()) sources.add("ELEVENST");

    LinkedHashMap<String, ProductItem> merged = new LinkedHashMap<>();
    for (ProductItem item : naverItems) {
      merged.put(item.productSq(), item);
    }
    for (ProductItem item : elevenItems) {
      merged.put(item.productSq(), item);
    }

    List<String> warnings = new ArrayList<>();
    if (naverItems.isEmpty()) warnings.add("네이버 상품 검색 결과를 불러오지 못했습니다.");
    if (elevenItems.isEmpty()) warnings.add("11번가 상품 검색 결과를 불러오지 못했습니다.");

    boolean live = !merged.isEmpty();
    List<ProductItem> sourceItems = live ? new ArrayList<>(merged.values()) : demoProducts(keyword);
    sortItems(sourceItems, sort, userSq);
    List<ProductItem> items = new ArrayList<>();
    for (ProductItem item : sourceItems) {
      if (items.size() >= size) {
        break;
      }
      items.add(item);
    }
    for (ProductItem item : items) {
      productCache.put(item.productSq(), item);
    }

    if (!live) {
      warnings.add("외부 상품 API가 연결되지 않아 서버 데모 데이터를 표시합니다.");
    }
    List<String> categories = new ArrayList<>();
    for (ProductItem item : naverItems) {
      for (String category : item.categories()) {
        if (!StringUtils.hasText(category) || categories.contains(category)) {
          continue;
        }
        categories.add(category);
        if (categories.size() >= 20) {
          break;
        }
      }
      if (categories.size() >= 20) {
        break;
      }
    }
    LinkedHashSet<String> suggestions = new LinkedHashSet<>(KEYWORDS);
    suggestions.addAll(categories);
    List<String> keywordSuggestions = new ArrayList<>(suggestions);
    return new SearchResponse(keyword, live, sources, warnings, keywordSuggestions, categories, items);
  }

  private void sortItems(List<ProductItem> items, SearchSort sort, Long userSq) {
    if (sort == SearchSort.POPULAR) {
      return;
    }
    if (sort == SearchSort.LOW_PRICE) {
      items.sort(Comparator.comparingLong(ProductItem::price));
      return;
    }
    Map<String, Long> clickCounts = new ConcurrentHashMap<>();
    items.sort(Comparator
        .comparingLong((ProductItem item) -> clickCounts.computeIfAbsent(
            item.source() + ":" + item.providerCode(),
            ignored -> userSq == null
                ? purchaseClickRepository.countByProviderAndExternalProductId(
                    item.source(), item.providerCode())
                : purchaseClickRepository.countByProviderAndExternalProductIdAndUserSq(
                    item.source(), item.providerCode(), userSq)))
        .reversed()
        .thenComparingLong(ProductItem::price));
  }

  private enum SearchSort {
    LOW_PRICE,
    POPULAR,
    PURCHASE;

    private static SearchSort from(String value) {
      if (!StringUtils.hasText(value)) {
        return LOW_PRICE;
      }
      try {
        return SearchSort.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
        return LOW_PRICE;
      }
    }
  }

  public HomeFeedResponse homeFeed() {
    return homeFeed(null);
  }

  public HomeFeedResponse homeFeed(Long userSq) {
    RecommendationProfile profile = recommendationProfile(userSq);
    List<String> keywords = profile.keywords();
    LinkedHashMap<String, ProductFeedItem> merged = new LinkedHashMap<>();
    List<String> warnings = new ArrayList<>();
    boolean live = false;

    for (String keyword : keywords) {
      List<ProductItem> naverItems = searchNaver(keyword, 10);
      if (naverItems.isEmpty()) {
        warnings.add("네이버 상품 검색 결과를 불러오지 못했습니다: " + keyword);
        continue;
      }
      live = true;
      collectSafely(keyword, naverItems);
      for (int index = 0; index < naverItems.size(); index++) {
        ProductFeedItem item = toInitialFeedItem(naverItems.get(index), index + 1);
        merged.putIfAbsent(item.productSq(), item);
      }
    }

    if (!live) {
      List<ProductItem> demoItems = demoProducts(keywords.getFirst());
      for (int index = 0; index < demoItems.size(); index++) {
        ProductFeedItem item = toInitialFeedItem(demoItems.get(index), index + 1);
        merged.put(item.productSq(), item);
      }
      warnings.add("네이버 상품 API가 연결되지 않아 서버 데모 데이터를 표시합니다.");
    }

    List<ProductFeedItem> rankedItems = new ArrayList<>(merged.values());
    List<ProductFeedItem> hotProducts = new ArrayList<>();
    for (ProductFeedItem item : rankedItems) {
      if (hotProducts.size() >= 4) {
        break;
      }
      hotProducts.add(item);
    }
    List<ProductFeedItem> bestPriceDeals = selectBestPriceDeals(rankedItems);
    return new HomeFeedResponse(
        live,
        warnings,
        userSq != null,
        profile.hasPurchaseHistory(),
        profile.basis(),
        keywords,
        hotProducts,
        bestPriceDeals
    );
  }

  public void collectDefaultPriceHistory() {
    for (String keyword : KEYWORDS) {
      collectSafely(keyword, searchNaver(keyword, 10));
    }
  }

  public ProductItem detail(String productSq) {
    ProductItem cached = productCache.get(productSq);
    if (cached != null) return cached;
    List<ProductItem> demoItems = demoProducts("추천");
    for (ProductItem item : demoItems) {
      if (item.productSq().equals(productSq)) {
        return item;
      }
    }
    return demoItems.getFirst();
  }

  @SuppressWarnings("unchecked")
  private List<ProductItem> searchNaver(String keyword, int size) {
    CProperties.Naver config = properties.getExternal().getNaver();
    if (!StringUtils.hasText(config.getClientId()) || !StringUtils.hasText(config.getClientSecret())) {
      return List.of();
    }

    try {
      URI uri = UriComponentsBuilder
          .fromUriString("https://openapi.naver.com/v1/search/shop.json")
          .queryParam("query", keyword)
          .queryParam("display", size)
          .queryParam("sort", "sim")
          .queryParam("exclude", "used:cbshop")
          .build()
          .toUri();
      Map<String, Object> response = RestClient.create()
          .get()
          .uri(uri)
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
            id,
            categoryValues(item),
            imageSources("NAVER", id, stringValue(item.get("image"), ""))
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
      URI uri = UriComponentsBuilder
          .fromUriString("https://openapi.11st.co.kr/openapi/OpenApiService.tmall")
          .queryParam("key", apiKey)
          .queryParam("apiCode", "ProductSearch")
          .queryParam("keyword", keyword)
          .queryParam("pageSize", size)
          .build()
          .toUri();
      String xml = RestClient.create()
          .get()
          .uri(uri)
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
        String cardImage = childText(product, "ProductImage200",
            childText(product, "ProductImage", ""));
        String largestImage = childText(product, "ProductImage300", cardImage);
        items.add(new ProductItem(
            "ELEVENST-" + id,
            childText(product, "ProductName", keyword),
            longValue(childText(product, "ProductPrice", "0")),
            childText(product, "SellerNick", "11번가"),
            "ELEVENST",
            largestImage,
            childText(product, "DetailPageUrl", ""),
            id,
            List.of(),
            imageSources("ELEVENST", id, largestImage)
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
        new ProductItem("DEMO-1", keyword + " 인기 상품", 28900, "통합 상품 검색", "DEMO", "", "https://search.shopping.naver.com/search/all?query=" + keyword, "SERVER_DEMO", List.of(), new ImageSources("", "", "", "")),
        new ProductItem("DEMO-2", keyword + " 실속형", 19800, "통합 상품 검색", "DEMO", "", "https://search.11st.co.kr/Search.tmall?kwd=" + keyword, "SERVER_DEMO", List.of(), new ImageSources("", "", "", "")),
        new ProductItem("DEMO-3", keyword + " 무료배송", 32500, "통합 상품 검색", "DEMO", "", "https://search.shopping.naver.com/search/all?query=" + keyword, "SERVER_DEMO", List.of(), new ImageSources("", "", "", "")),
        new ProductItem("DEMO-4", keyword + " 대용량", 41900, "통합 상품 검색", "DEMO", "", "https://search.11st.co.kr/Search.tmall?kwd=" + keyword, "SERVER_DEMO", List.of(), new ImageSources("", "", "", ""))
    );
  }

  private List<String> categoryValues(Map<?, ?> item) {
    List<String> categories = new ArrayList<>();
    for (String key : List.of("category1", "category2", "category3", "category4")) {
      String category = stringValue(item.get(key), "");
      if (StringUtils.hasText(category)) {
        categories.add(category);
      }
    }
    return List.copyOf(categories);
  }

  private ImageSources imageSources(String source, String providerCode, String original) {
    if (!providerCode.matches("[A-Za-z0-9_-]+")) {
      return new ImageSources(original, original, original, original);
    }
    productImageService.remember(source, providerCode, original);
    if ("ELEVENST".equals(source) && original.contains("/11dims/resize/x")) {
      return new ImageSources(
          elevenstImage(original, 320),
          elevenstImage(original, 640),
          elevenstImage(original, 960),
          original
      );
    }
    String base = "/delivery/wp/product-images/" + source + "/" + providerCode;
    return new ImageSources(
        base + "?width=320",
        base + "?width=640",
        base + "?width=960",
        original
    );
  }

  private String elevenstImage(String original, int width) {
    return original.replaceFirst("/11dims/resize/x\\d+/", "/11dims/resize/x" + width + "/");
  }

  private void collectSafely(String keyword, List<ProductItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    productCatalogMessagePublisher.publish(
        ProductCatalogCollectionType.COLLECT_AND_ENRICH,
        keyword,
        items
    );
  }

  private void registerSafely(String keyword, List<ProductItem> items) {
    if (items == null || items.isEmpty()) {
      return;
    }
    productCatalogMessagePublisher.publish(
        ProductCatalogCollectionType.REGISTER_OFFERS,
        keyword,
        items
    );
  }

  private List<ProductFeedItem> selectBestPriceDeals(List<ProductFeedItem> items) {
    List<ProductFeedItem> historicalLowItems = new ArrayList<>();
    for (ProductFeedItem item : items) {
      if (item.historicalLow()) {
        historicalLowItems.add(item);
      }
    }
    historicalLowItems.sort(new Comparator<ProductFeedItem>() {
      @Override
      public int compare(ProductFeedItem first, ProductFeedItem second) {
        return Integer.compare(first.rank(), second.rank());
      }
    });
    List<ProductFeedItem> selected = new ArrayList<>();
    for (ProductFeedItem item : historicalLowItems) {
      if (selected.size() >= 3) {
        break;
      }
      selected.add(item);
    }
    if (selected.size() < 3) {
      Set<String> selectedIds = new LinkedHashSet<>();
      for (ProductFeedItem item : selected) {
        selectedIds.add(item.productSq());
      }
      List<ProductFeedItem> remainingItems = new ArrayList<>();
      for (ProductFeedItem item : items) {
        if (!selectedIds.contains(item.productSq())) {
          remainingItems.add(item);
        }
      }
      remainingItems.sort(new Comparator<ProductFeedItem>() {
        @Override
        public int compare(ProductFeedItem first, ProductFeedItem second) {
          return Long.compare(first.price(), second.price());
        }
      });
      for (ProductFeedItem item : remainingItems) {
        if (selected.size() >= 3) {
          break;
        }
        selected.add(item);
      }
    }
    return selected;
  }

  private ProductFeedItem toInitialFeedItem(ProductItem item, int rank) {
    return new ProductFeedItem(
        item.productSq(),
        item.name(),
        item.price(),
        item.mallName(),
        item.source(),
        item.imageUrl(),
        item.productUrl(),
        item.providerCode(),
        item.imageSources(),
        rank,
        item.price(),
        false,
        "COLLECTING",
        "NAVER_LPRICE",
        0
    );
  }

  private RecommendationProfile recommendationProfile(Long userSq) {
    if (userSq == null) {
      return new RecommendationProfile(List.of("생활용품"), false, "POPULAR");
    }

    LinkedHashSet<String> purchaseKeywords = new LinkedHashSet<>();
    List<PurchaseClickEntity> clicks = purchaseClickRepository.findAllByUserSqOrderByClickedAtDesc(userSq);
    for (PurchaseClickEntity click : clicks) {
      ProductEntity product = productRepository.findById(click.getProductSq()).orElse(null);
      if (product == null) {
        continue;
      }
      String context = (stringValue(product.getName(), "") + " "
          + stringValue(product.getCategoryPath(), "")).toUpperCase(Locale.ROOT);
      addInterestKeywords(context, purchaseKeywords);
      if (purchaseKeywords.size() >= 3) {
        break;
      }
    }
    if (!purchaseKeywords.isEmpty()) {
      return new RecommendationProfile(limitKeywords(purchaseKeywords), true, "PURCHASE_HISTORY");
    }

    List<WikiEntryEntity> entries = wikiEntryRepository
        .findAllByUserSqAndStatusOrderByModifiedDateDesc(userSq, WikiEntryStatusCode.ACTIVE);
    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    for (WikiEntryEntity entry : entries) {
      String context = (entry.getEntryKey() + " " + entry.getSummary() + " " + entry.getContentJson())
          .toUpperCase(Locale.ROOT);
      addInterestKeywords(context, keywords);
      if (keywords.size() >= 3) {
        break;
      }
    }
    if (keywords.isEmpty()) {
      keywords.add("생활용품");
    }
    return new RecommendationProfile(limitKeywords(keywords), false,
        entries.isEmpty() ? "POPULAR" : "PERSONAL_WIKI");
  }

  private List<String> limitKeywords(Set<String> keywords) {
    List<String> limitedKeywords = new ArrayList<>();
    for (String keyword : keywords) {
      if (limitedKeywords.size() >= 3) {
        break;
      }
      limitedKeywords.add(keyword);
    }
    return List.copyOf(limitedKeywords);
  }

  private void addInterestKeywords(String context, Set<String> keywords) {
    if (context.contains("CAT") || context.contains("고양이")) {
      keywords.add("고양이 사료");
      keywords.add("고양이 모래");
    }
    if (context.contains("DOG") || context.contains("강아지")) {
      keywords.add("강아지 사료");
    }
    if (context.contains("PET_FOOD") || context.contains("반려동물 사료")) {
      keywords.add("반려동물 사료");
    }
    if (context.contains("PET_SUPPLY") || context.contains("반려동물 용품")) {
      keywords.add("반려동물 용품");
    }
    if (context.contains("HOUSEHOLD") || context.contains("생활용품")) {
      keywords.add("생활용품");
    }
    if (context.contains("세제") || context.contains("세탁")) {
      keywords.add("세탁세제");
    }
    if (context.contains("커피")) {
      keywords.add("커피");
    }
    if (context.contains("CAMPING") || context.contains("캠핑")) {
      keywords.add("캠핑 용품");
    }
    if (context.contains("BABY") || context.contains("육아")) {
      keywords.add("유아 용품");
    }
  }

  private record RecommendationProfile(
      List<String> keywords,
      boolean hasPurchaseHistory,
      String basis
  ) {
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
