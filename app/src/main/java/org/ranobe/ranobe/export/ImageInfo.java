package org.ranobe.ranobe.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Type of an image, read from its first bytes only.
 */
final class ImageInfo {
    static final String JPEG = "jpg";
    static final String PNG = "png";
    static final String GIF = "gif";
    static final String WEBP = "webp";

    private ImageInfo() {
    }

    /**
     * The formats every EPUB reader must support, anything else is converted first.
     */
    static boolean isEpubCore(String format) {
        return JPEG.equals(format) || PNG.equals(format) || GIF.equals(format);
    }

    static String mediaType(String format) {
        switch (format) {
            case PNG:
                return "image/png";
            case GIF:
                return "image/gif";
            case WEBP:
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    /**
     * Returns null when the file isn't an image we recognise.
     */
    static String format(File file) throws IOException {
        byte[] head = new byte[12];
        int read = 0;
        try (InputStream in = new FileInputStream(file)) {
            while (read < head.length) {
                int n = in.read(head, read, head.length - read);
                if (n < 0) break;
                read += n;
            }
        }
        return read < head.length ? null : sniff(head);
    }

    static String sniff(byte[] h) {
        if ((h[0] & 0xff) == 0xFF && (h[1] & 0xff) == 0xD8 && (h[2] & 0xff) == 0xFF) return JPEG;
        if ((h[0] & 0xff) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return PNG;
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return GIF;
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return WEBP;
        return null;
    }
}
