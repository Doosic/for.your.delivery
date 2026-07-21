package com.foryour.delivery.client.product;

import com.foryour.delivery.domain.entity.ProductEntity;
import com.foryour.delivery.domain.entity.ProductOfferEntity;
import com.foryour.delivery.domain.repository.ProductOfferRepository;
import com.foryour.delivery.domain.repository.ProductRepository;
import com.foryour.delivery.exception.APIException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.foryour.delivery.domain.enums.ErrorCode.BAD_REQUEST;
import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class ProductImageService {

  private static final Set<Integer> ALLOWED_WIDTHS = Set.of(320, 640, 960);
  private static final Set<String> ALLOWED_HOSTS = Set.of(
      "shopping-phinf.pstatic.net",
      "cdn.011st.com"
  );
  private static final int MAX_SOURCE_BYTES = 8 * 1024 * 1024;
  private static final int MAX_CACHE_ITEMS = 500;

  private final ProductRepository productRepository;
  private final ProductOfferRepository productOfferRepository;
  private final Map<String, byte[]> imageCache = new ConcurrentHashMap<>();
  private final Map<String, String> sourceUrls = new ConcurrentHashMap<>();

  public void remember(String provider, String providerCode, String imageUrl) {
    if (provider == null || providerCode == null || imageUrl == null || imageUrl.isBlank()) {
      return;
    }
    String key = sourceKey(provider, providerCode);
    sourceUrls.put(key, imageUrl);
    for (int width : ALLOWED_WIDTHS) {
      imageCache.remove(key + ":" + width);
    }
  }

  public byte[] resizedImage(String provider, String providerCode, int width) {
    if (!ALLOWED_WIDTHS.contains(width) || provider == null || providerCode == null) {
      throw new APIException(BAD_REQUEST);
    }
    String normalizedProvider = provider.toUpperCase();
    String cacheKey = sourceKey(normalizedProvider, providerCode) + ":" + width;
    byte[] cached = imageCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    byte[] resized;
    try {
      String imageUrl = resolveImageUrl(normalizedProvider, providerCode);
      validateImageUrl(imageUrl);
      byte[] source = RestClient.create()
          .get()
          .uri(imageUrl)
          .retrieve()
          .body(byte[].class);
      if (source == null || source.length == 0 || source.length > MAX_SOURCE_BYTES) {
        throw new APIException(BAD_REQUEST);
      }
      resized = resizeJpeg(source, width);
    } catch (RuntimeException error) {
      resized = placeholderJpeg(width);
    }

    if (imageCache.size() >= MAX_CACHE_ITEMS) {
      imageCache.clear();
    }
    imageCache.put(cacheKey, resized);
    return resized;
  }

  private String resolveImageUrl(String provider, String providerCode) {
    String remembered = sourceUrls.get(sourceKey(provider, providerCode));
    if (remembered != null) {
      return remembered;
    }
    Optional<ProductOfferEntity> offer = productOfferRepository
        .findByProviderAndExternalProductId(provider, providerCode);
    if (offer.isPresent()) {
      return productRepository.findById(offer.get().getProductSq())
          .map(ProductEntity::getImageUrl)
          .orElseThrow(() -> new APIException(DATA_NOT_EXIST));
    }
    return productRepository.findByProductKey(provider + ":" + providerCode)
        .map(ProductEntity::getImageUrl)
        .orElseThrow(() -> new APIException(DATA_NOT_EXIST));
  }

  private String sourceKey(String provider, String providerCode) {
    return provider.toUpperCase() + ":" + providerCode;
  }

  private void validateImageUrl(String imageUrl) {
    try {
      URI uri = URI.create(imageUrl);
      if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_HOSTS.contains(uri.getHost())) {
        throw new APIException(BAD_REQUEST);
      }
    } catch (IllegalArgumentException error) {
      throw new APIException(BAD_REQUEST);
    }
  }

  private byte[] resizeJpeg(byte[] source, int requestedWidth) {
    try {
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
      if (original == null) {
        throw new APIException(BAD_REQUEST);
      }
      int targetWidth = Math.min(requestedWidth, original.getWidth());
      int targetHeight = Math.max(1, (int) Math.round(
          original.getHeight() * (targetWidth / (double) original.getWidth())));
      BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = resized.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
      graphics.dispose();
      return writeJpeg(resized);
    } catch (APIException error) {
      throw error;
    } catch (Exception error) {
      throw new APIException(BAD_REQUEST);
    }
  }

  private byte[] placeholderJpeg(int width) {
    int height = Math.max(1, (int) Math.round(width * 0.75));
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(new Color(241, 245, 249));
    graphics.fillRect(0, 0, width, height);
    graphics.setColor(new Color(14, 116, 144));
    graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(20, width / 10)));
    String label = "FUB";
    int textWidth = graphics.getFontMetrics().stringWidth(label);
    int baseline = (height + graphics.getFontMetrics().getAscent()) / 2;
    graphics.drawString(label, (width - textWidth) / 2, baseline);
    graphics.dispose();
    return writeJpeg(image);
  }

  private byte[] writeJpeg(BufferedImage image) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) {
        throw new APIException(BAD_REQUEST);
      }
      ImageWriter writer = writers.next();
      try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
        writer.setOutput(imageOutput);
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(0.92f);
        writer.write(null, new IIOImage(image, null, null), params);
      } finally {
        writer.dispose();
      }
      return output.toByteArray();
    } catch (APIException error) {
      throw error;
    } catch (Exception error) {
      throw new APIException(BAD_REQUEST);
    }
  }
}
