package com.foryour.delivery.client.product;

import com.foryour.delivery.domain.entity.ProductEntity;
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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.foryour.delivery.domain.enums.ErrorCode.BAD_REQUEST;
import static com.foryour.delivery.domain.enums.ErrorCode.DATA_NOT_EXIST;

@Service
@RequiredArgsConstructor
public class ProductImageService {

  private static final Set<Integer> ALLOWED_WIDTHS = Set.of(320, 640, 960);
  private static final Set<String> ALLOWED_HOSTS = Set.of("shopping-phinf.pstatic.net");
  private static final int MAX_SOURCE_BYTES = 8 * 1024 * 1024;
  private static final int MAX_CACHE_ITEMS = 500;

  private final ProductRepository productRepository;
  private final Map<String, byte[]> imageCache = new ConcurrentHashMap<>();

  public byte[] resizedNaverImage(String providerCode, int width) {
    if (!ALLOWED_WIDTHS.contains(width)) {
      throw new APIException(BAD_REQUEST);
    }
    String cacheKey = providerCode + ":" + width;
    byte[] cached = imageCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }

    ProductEntity product = productRepository.findByProductKey("NAVER:" + providerCode)
        .orElseThrow(() -> new APIException(DATA_NOT_EXIST));
    validateImageUrl(product.getImageUrl());

    byte[] source = RestClient.create()
        .get()
        .uri(product.getImageUrl())
        .retrieve()
        .body(byte[].class);
    if (source == null || source.length == 0 || source.length > MAX_SOURCE_BYTES) {
      throw new APIException(BAD_REQUEST);
    }

    byte[] resized = resizeJpeg(source, width);
    if (imageCache.size() >= MAX_CACHE_ITEMS) {
      imageCache.clear();
    }
    imageCache.put(cacheKey, resized);
    return resized;
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

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
      ImageWriter writer = writers.next();
      try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
        writer.setOutput(imageOutput);
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(0.92f);
        writer.write(null, new IIOImage(resized, null, null), params);
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
