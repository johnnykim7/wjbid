package com.biddingagency.domain.sharedfile.service;

import com.biddingagency.domain.sharedfile.SharedFileError;
import com.biddingagency.domain.sharedfile.SharedFileException;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;

/**
 * 파일을 조각마다 AES-GCM 으로 암호화한다. 한 번에 메모리에 올리지 않으면서 받기 도중에도 변조를 잡기 위해서다.
 * 키가 파일마다 새로 나오므로 nonce 는 조각 번호로 충분하고, 마지막 조각 표시를 넣어 뒷부분이 잘린 파일을 거부한다.
 */
@Component
public class SharedFileCrypto {

    public static final int CHUNK_BYTES = 1 << 20;

    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int PASSWORD_LENGTH = 20;
    private static final int PASSWORD_GROUP = 5;
    private static final int SALT_BYTES = 16;
    private static final int KDF_ITERATIONS = 210_000;
    private static final int DERIVED_BITS = 512;
    private static final int KEY_BYTES = 32;
    private static final int TAG_BYTES = 16;
    private static final int NONCE_BYTES = 12;
    private static final int NONCE_COUNTER_OFFSET = 7;

    private final SecureRandom random = new SecureRandom();

    public record Keys(byte[] encryptionKey, byte[] passwordCheck) {
    }

    public String newPassword() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            if (i > 0 && i % PASSWORD_GROUP == 0) {
                out.append('-');
            }
            out.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return out.toString();
    }

    public byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    /** 메일에서 옮겨 적을 때 섞이는 공백, 붙임표, 소문자를 없앤다. */
    public static String normalize(String password) {
        return password == null ? "" : password.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    public Keys derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(normalize(password).toCharArray(), salt, KDF_ITERATIONS, DERIVED_BITS);
            byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            byte[] key = Arrays.copyOfRange(derived, 0, KEY_BYTES);
            byte[] check = MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.copyOfRange(derived, KEY_BYTES, derived.length));
            return new Keys(key, check);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("키를 만들지 못했습니다", e);
        }
    }

    public static boolean matches(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(expected, actual);
    }

    public InputStream encrypting(InputStream plain, byte[] key) throws IOException {
        return new EncryptingStream(plain, key);
    }

    public void decrypt(InputStream cipher, OutputStream plain, byte[] key) throws IOException {
        byte[] current = cipher.readNBytes(CHUNK_BYTES + TAG_BYTES);
        if (current.length == 0) {
            throw new SharedFileException(SharedFileError.CORRUPT);
        }
        int counter = 0;
        while (true) {
            byte[] next = cipher.readNBytes(CHUNK_BYTES + TAG_BYTES);
            boolean last = next.length == 0;
            plain.write(open(key, counter++, last, current));
            if (last) {
                return;
            }
            current = next;
        }
    }

    private static byte[] seal(byte[] key, int counter, boolean last, byte[] plain) {
        try {
            return cipher(Cipher.ENCRYPT_MODE, key, counter, last).doFinal(plain);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화하지 못했습니다", e);
        }
    }

    private static byte[] open(byte[] key, int counter, boolean last, byte[] sealed) {
        try {
            return cipher(Cipher.DECRYPT_MODE, key, counter, last).doFinal(sealed);
        } catch (GeneralSecurityException e) {
            throw new SharedFileException(SharedFileError.CORRUPT);
        }
    }

    private static Cipher cipher(int mode, byte[] key, int counter, boolean last) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_BYTES];
        ByteBuffer.wrap(nonce).putInt(NONCE_COUNTER_OFFSET, counter);
        nonce[NONCE_BYTES - 1] = (byte) (last ? 1 : 0);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BYTES * Byte.SIZE, nonce));
        return cipher;
    }

    private static final class EncryptingStream extends InputStream {

        private final InputStream source;
        private final byte[] key;
        private byte[] upcoming;
        private byte[] current = new byte[0];
        private int position;
        private int counter;
        private boolean finished;

        private EncryptingStream(InputStream source, byte[] key) throws IOException {
            this.source = source;
            this.key = key;
            this.upcoming = source.readNBytes(CHUNK_BYTES);
        }

        @Override
        public int read() throws IOException {
            return ensureAvailable() ? current[position++] & 0xff : -1;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            if (!ensureAvailable()) {
                return -1;
            }
            int count = Math.min(length, current.length - position);
            System.arraycopy(current, position, target, offset, count);
            position += count;
            return count;
        }

        private boolean ensureAvailable() throws IOException {
            if (position < current.length) {
                return true;
            }
            if (finished) {
                return false;
            }
            byte[] plain = upcoming;
            upcoming = source.readNBytes(CHUNK_BYTES);
            finished = upcoming.length == 0;
            current = seal(key, counter++, finished, plain);
            position = 0;
            return true;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }
}
