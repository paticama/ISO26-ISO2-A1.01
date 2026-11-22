package com.sescam.radiodiagnostics.integration.secret;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the credentials encrypted (AES-256-GCM) so that they are protected in storage. The key lives only in
 * memory, like the data, until the database and key management decisions exist. Error messages never include
 * the secret or the underlying exception text.
 */
public class EncryptedInMemorySecretStore implements SecretStore {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, byte[]> encrypted = new ConcurrentHashMap<>();

    public EncryptedInMemorySecretStore() {
        this(newKey());
    }

    public EncryptedInMemorySecretStore(SecretKey key) {
        this.key = Objects.requireNonNull(key, "key must not be null");
    }

    @Override
    public void put(String integrationId, String secret) {
        Objects.requireNonNull(integrationId, "integrationId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            byte[] stored = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, stored, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, stored, IV_BYTES, cipherText.length);
            encrypted.put(integrationId, stored);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("The credential could not be stored.");
        }
    }

    @Override
    public Optional<String> get(String integrationId) {
        byte[] stored = encrypted.get(integrationId);
        if (stored == null) {
            return Optional.empty();
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(stored, 0, IV_BYTES)));
            byte[] plain = cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
            return Optional.of(new String(plain, StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("The credential could not be read.");
        }
    }

    @Override
    public boolean contains(String integrationId) {
        return encrypted.containsKey(integrationId);
    }

    /** The stored bytes (IV + cipher text), for tests that check the secret is not kept in clear. */
    byte[] rawBytes(String integrationId) {
        return encrypted.get(integrationId);
    }

    private static SecretKey newKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            return generator.generateKey();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES is not available.");
        }
    }
}
