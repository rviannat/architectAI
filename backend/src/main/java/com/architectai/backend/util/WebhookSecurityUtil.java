package com.architectai.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utility for validating GitHub webhook signatures using HMAC-SHA256.
 */
public class WebhookSecurityUtil {
    private static final Logger log = LoggerFactory.getLogger(WebhookSecurityUtil.class);
    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    /**
     * Validates the X-Hub-Signature-256 header against the request body using HMAC-SHA256.
     *
     * @param body the raw request body (JSON string)
     * @param secret the webhook secret
     * @param signature the X-Hub-Signature-256 header value (e.g., "sha256=...")
     * @return true if signature is valid, false otherwise
     */
    public static boolean isValidSignature(String body, String secret, String signature) {
        if (body == null || body.isEmpty()) {
            log.warn("Webhook validation: body is empty");
            return false;
        }

        if (secret == null || secret.isEmpty()) {
            log.warn("Webhook validation: secret is empty");
            return false;
        }

        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook validation: signature is missing");
            return false;
        }

        try {
            String computed = computeHmacSha256(body, secret);
            boolean isValid = computed.equalsIgnoreCase(signature);

            if (!isValid) {
                log.warn("Webhook validation failed: signature mismatch");
                log.debug("Expected: {} | Received: {}", computed, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Computes HMAC-SHA256 signature for the given body and secret.
     *
     * @param body the request body
     * @param secret the webhook secret
     * @return the computed signature in the format "sha256=..." (lowercase hex)
     */
    public static String computeHmacSha256(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        mac.init(key);

        byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        String hex = bytesToHex(hash);

        return PREFIX + hex;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}

