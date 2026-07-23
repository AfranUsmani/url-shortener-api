package io.github.afranusmani.urlshortener.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Renders QR codes (PNG) for short links using ZXing. Stateless and cheap, so
 * it is called directly on the QR endpoint without caching.
 */
@Service
public class QrCodeService {

    private static final int MIN_SIZE = 64;
    private static final int MAX_SIZE = 1000;

    /**
     * @param content the text/URL to encode
     * @param size    requested edge length in pixels (clamped to a sane range)
     * @return PNG bytes
     */
    public byte[] pngFor(String content, int size) {
        int px = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 1,
                EncodeHintType.CHARACTER_SET, "UTF-8"
        );
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            // Should not happen for valid short URLs; surface as a 500 via the container.
            throw new IllegalStateException("Failed to render QR code", e);
        }
    }
}
