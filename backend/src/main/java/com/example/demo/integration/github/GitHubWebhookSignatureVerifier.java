package com.example.demo.integration.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GitHubWebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM =
            "HmacSHA256";

    private static final String SIGNATURE_PREFIX =
            "sha256=";

    private final byte[] secret;


    public GitHubWebhookSignatureVerifier(
            @Value("${app.github.webhook-secret:}")
            String secret
    ) {

        this.secret =
                secret == null
                        ? new byte[0]
                        : secret.getBytes(
                        StandardCharsets.UTF_8
                );
    }


    public boolean isValid(
            byte[] payload,
            String signatureHeader
    ) {

        if (
                payload == null
                        ||
                        signatureHeader == null
                        ||
                        signatureHeader.isBlank()
                        ||
                        secret.length == 0
                        ||
                        !signatureHeader.startsWith(
                                SIGNATURE_PREFIX
                        )
        ) {
            return false;
        }


        try {

            Mac mac =
                    Mac.getInstance(
                            HMAC_ALGORITHM
                    );

            mac.init(
                    new SecretKeySpec(
                            secret,
                            HMAC_ALGORITHM
                    )
            );


            byte[] expectedSignature =
                    mac.doFinal(
                            payload
                    );


            String receivedHex =
                    signatureHeader.substring(
                            SIGNATURE_PREFIX.length()
                    );


            byte[] receivedSignature;

            try {

                receivedSignature =
                        HexFormat
                                .of()
                                .parseHex(
                                        receivedHex
                                );

            } catch (IllegalArgumentException exception) {

                return false;
            }


            return MessageDigest.isEqual(
                    expectedSignature,
                    receivedSignature
            );

        } catch (GeneralSecurityException exception) {

            throw new IllegalStateException(
                    "GitHub Webhook 서명 검증에 실패했습니다.",
                    exception
            );
        }
    }
}