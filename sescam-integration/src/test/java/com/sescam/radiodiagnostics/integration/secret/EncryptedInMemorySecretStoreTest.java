package com.sescam.radiodiagnostics.integration.secret;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedInMemorySecretStoreTest {

    @Test
    @DisplayName("A stored credential can be read back and is not kept in clear text")
    void storedEncrypted() {
        EncryptedInMemorySecretStore store = new EncryptedInMemorySecretStore();

        store.put("sso", "S3cret-Value-123");

        assertThat(store.contains("sso")).isTrue();
        assertThat(store.get("sso")).contains("S3cret-Value-123");
        byte[] raw = store.rawBytes("sso");
        assertThat(new String(raw, StandardCharsets.ISO_8859_1)).doesNotContain("S3cret-Value-123");
    }

    @Test
    @DisplayName("Same credential encrypts differently each time (random IV)")
    void randomIv() {
        EncryptedInMemorySecretStore store = new EncryptedInMemorySecretStore();
        store.put("a", "Same-Secret-1");
        store.put("b", "Same-Secret-1");

        assertThat(Arrays.equals(store.rawBytes("a"), store.rawBytes("b"))).isFalse();
    }

    @Test
    @DisplayName("Unknown id gives empty; a new credential replaces the old one")
    void unknownAndReplace() {
        EncryptedInMemorySecretStore store = new EncryptedInMemorySecretStore();
        assertThat(store.get("nope")).isEmpty();
        assertThat(store.contains("nope")).isFalse();

        store.put("sso", "First-Secret-1");
        store.put("sso", "Second-Secret-2");

        assertThat(store.get("sso")).contains("Second-Secret-2");
    }

    @Test
    @DisplayName("Null arguments are rejected; a store can be built with a given key")
    void guardsAndGivenKey() throws Exception {
        EncryptedInMemorySecretStore store = new EncryptedInMemorySecretStore();
        assertThatThrownBy(() -> store.put(null, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> store.put("a", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EncryptedInMemorySecretStore(null)).isInstanceOf(NullPointerException.class);

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey other = generator.generateKey();
        EncryptedInMemorySecretStore first = new EncryptedInMemorySecretStore(other);
        first.put("a", "Some-Secret-123");
        assertThat(first.get("a")).contains("Some-Secret-123");
    }
}
