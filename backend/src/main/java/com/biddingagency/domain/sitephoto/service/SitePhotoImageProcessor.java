package com.biddingagency.domain.sitephoto.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 올린 사진을 고객 화면에 맞는 크기와 무게의 JPEG 로 다시 만든다 (CR-153, BIZ-068)
 *
 * 그림을 다 풀기 전에 크기부터 읽어, 작은 파일에 큰 화면을 숨긴 사진이 서버 메모리를 다 쓰지 않게 한다.
 * 다시 저장하므로 원본의 촬영 위치 같은 정보는 남지 않는다.
 */
@Component
public class SitePhotoImageProcessor {

    private static final float START_QUALITY = 0.85f;
    private static final float MIN_QUALITY = 0.6f;
    private static final float QUALITY_STEP = 0.05f;

    public record Result(byte[] jpeg, int width, int height, int originalWidth, int originalHeight) {
    }

    public Result process(byte[] content, SitePhotoRules rules) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw notAllowed(rules);
            }
            ImageReader reader = readers.next();
            try {
                if (!isAllowedFormat(reader, rules)) {
                    throw notAllowed(rules);
                }
                reader.setInput(in, true, true);
                int rawWidth = reader.getWidth(0);
                int rawHeight = reader.getHeight(0);
                if ((long) rawWidth * rawHeight > rules.maxPixels()) {
                    throw badRequest("화소 수가 너무 많은 사진입니다. " + rules.maxMegapixels() * 100 + "만 화소 이하 사진을 올려 주세요");
                }

                int orientation = readOrientation(content);
                boolean turnsSideways = orientation >= 5 && orientation <= 8;
                int originalWidth = turnsSideways ? rawHeight : rawWidth;
                int originalHeight = turnsSideways ? rawWidth : rawHeight;
                if (originalWidth < rules.minWidth()) {
                    throw badRequest("가로 " + originalWidth + " 사진이라 흐릿하게 보입니다. 가로 " + rules.minWidth() + " 이상 사진을 올려 주세요");
                }

                BufferedImage upright = orient(reader.read(0), orientation);
                BufferedImage flat = flattenOnWhite(resize(upright, rules.saveWidth()));
                byte[] jpeg = encode(flat, rules.saveMaxBytes());
                return new Result(jpeg, flat.getWidth(), flat.getHeight(), originalWidth, originalHeight);
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw notAllowed(rules);
        }
    }

    private boolean isAllowedFormat(ImageReader reader, SitePhotoRules rules) {
        String[] mimeTypes = reader.getOriginatingProvider() == null ? null : reader.getOriginatingProvider().getMIMETypes();
        return mimeTypes != null && Arrays.stream(mimeTypes).map(String::toLowerCase).anyMatch(rules.allowedTypes()::contains);
    }

    /** EXIF Orientation. 없거나 읽지 못하면 1(그대로) */
    private int readOrientation(byte[] content) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(content));
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (exif != null && exif.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return exif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (ImageProcessingException | IOException | MetadataException e) {
            return 1;
        }
        return 1;
    }

    /** 휴대폰 사진이 옆으로 눕지 않게 촬영 방향대로 바로 세운다 */
    private BufferedImage orient(BufferedImage src, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        AffineTransform t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.translate(w, 0); t.scale(-1, 1); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.translate(0, h); t.scale(1, -1); }
            case 5 -> { t.rotate(-Math.PI / 2); t.scale(-1, 1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.translate(h, w); t.scale(-1, -1); t.rotate(-Math.PI / 2); t.scale(-1, 1); }
            default -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
        }
        boolean sideways = orientation >= 5;
        BufferedImage dst = new BufferedImage(sideways ? h : w, sideways ? w : h, imageType(src));
        Graphics2D g = dst.createGraphics();
        try {
            g.drawImage(src, t, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /** 한 번에 크게 줄이면 계단이 생겨 반씩 여러 번 줄인 뒤 마지막에 맞춘다 */
    private BufferedImage resize(BufferedImage src, int targetWidth) {
        if (src.getWidth() <= targetWidth) {
            return src;
        }
        int targetHeight = Math.max(1, Math.round((float) src.getHeight() * targetWidth / src.getWidth()));
        BufferedImage current = src;
        while (current.getWidth() / 2 >= targetWidth) {
            current = draw(current, current.getWidth() / 2, Math.max(1, current.getHeight() / 2));
        }
        return draw(current, targetWidth, targetHeight);
    }

    private BufferedImage draw(BufferedImage src, int width, int height) {
        BufferedImage dst = new BufferedImage(width, height, imageType(src));
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(src, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /** JPEG 는 투명을 담지 못해 투명한 곳을 흰 바탕으로 채운다 */
    private BufferedImage flattenOnWhite(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /** 화질을 낮춰 가며 무게 목표 안에 들면 멈춘다. 가장 낮은 화질에서도 넘으면 그것으로 저장한다 */
    private byte[] encode(BufferedImage image, long maxBytes) throws IOException {
        byte[] result = null;
        for (int step = 0; ; step++) {
            float quality = START_QUALITY - QUALITY_STEP * step;
            if (quality < MIN_QUALITY - 0.001f) {
                return result;
            }
            result = writeJpeg(image, quality);
            if (result.length <= maxBytes) {
                return result;
            }
        }
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private int imageType(BufferedImage src) {
        return src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
    }

    private ResponseStatusException notAllowed(SitePhotoRules rules) {
        return badRequest(rules.allowedTypesLabel() + " 사진만 올릴 수 있습니다");
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
