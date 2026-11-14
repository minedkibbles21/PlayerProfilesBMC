package com.muhammaddaffa.playerprofiles.proxy;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SecretManager {

    public static final String PEM_HEADER = "-----BEGIN BMC PROXY SECRET KEY-----";
    public static final String PEM_FOOTER = "-----END BMC PROXY SECRET KEY-----";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final File secretFileLocation;
    private byte[] secretKeyBytes;

    public SecretManager(File baseDirectory, String secretFileName) {
        this.secretFileLocation = new File(baseDirectory, secretFileName != null ? secretFileName : "secret.pem");
    }

    public File getSecretFile() {
        return this.secretFileLocation;
    }

    public byte[] getSecretKeyBytes() {
        return this.secretKeyBytes;
    }

    public synchronized void ensureSecretLoaded() throws IOException {
        if (!this.secretFileLocation.exists()) {
            File parentDir = this.secretFileLocation.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            SecureRandom randomGenerator = new SecureRandom();
            byte[] generatedKeyBytes = new byte[32];
            randomGenerator.nextBytes(generatedKeyBytes);
            this.secretKeyBytes = generatedKeyBytes;

            String base64Encoded = Base64.getEncoder().encodeToString(generatedKeyBytes);
            List<String> outputLines = new ArrayList<>();
            outputLines.add(PEM_HEADER);
            outputLines.add(base64Encoded);
            outputLines.add(PEM_FOOTER);

            Files.write(this.secretFileLocation.toPath(), outputLines, StandardCharsets.UTF_8);
            return;
        }

        List<String> existingLines = Files.readAllLines(this.secretFileLocation.toPath(), StandardCharsets.UTF_8);
        StringBuilder contentBuilder = new StringBuilder();
        for (String currentLine : existingLines) {
            String trimmedLine = currentLine.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("-----")) {
                continue;
            }
            contentBuilder.append(trimmedLine);
        }

        String rawContent = contentBuilder.toString();
        try {
            this.secretKeyBytes = Base64.getDecoder().decode(rawContent);
        } catch (IllegalArgumentException decodeException) {
            this.secretKeyBytes = rawContent.getBytes(StandardCharsets.UTF_8);
        }
    }

    public byte[] computeHmac(byte[] messagePayload) {
        if (this.secretKeyBytes == null || messagePayload == null) {
            return new byte[32];
        }
        try {
            Mac macCalculator = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretSpec = new SecretKeySpec(this.secretKeyBytes, HMAC_ALGORITHM);
            macCalculator.init(secretSpec);
            return macCalculator.doFinal(messagePayload);
        } catch (Exception calculationException) {
            return new byte[32];
        }
    }

    public boolean verifyHmac(byte[] messagePayload, byte[] incomingSignature) {
        if (incomingSignature == null || incomingSignature.length != 32) {
            return false;
        }
        byte[] expectedSignature = computeHmac(messagePayload);
        return MessageDigest.isEqual(expectedSignature, incomingSignature);
    }
}
