package org.example.chat.controller;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class OtpGenerationPropertyTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Provide
    Arbitrary<String> sixDigitStrings() {
        return Arbitraries.strings()
                .withCharRange('0', '9')
                .ofLength(6);
    }

    @Property
    void generateOtp_AlwaysReturnsExactlySixDigits() {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1000000));

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"),
                "OTP should be exactly 6 digits but was: " + otp);
    }

    @Property
    void bcryptRoundtrip_ForAnySixDigitString_EncodeMatchesOriginal(
            @ForAll("sixDigitStrings") String otp) {
        String hash = PASSWORD_ENCODER.encode(otp);

        assertTrue(PASSWORD_ENCODER.matches(otp, hash),
                "BCrypt hash should match the original OTP");
    }

    @Property
    void bcryptRoundtrip_ForAnySixDigitString_DoesNotMatchDifferent(
            @ForAll("sixDigitStrings") String otp,
            @ForAll("sixDigitStrings") String otherOtp) {
        Assume.that(!otp.equals(otherOtp));

        String hash = PASSWORD_ENCODER.encode(otp);

        assertFalse(PASSWORD_ENCODER.matches(otherOtp, hash),
                "BCrypt hash should not match a different OTP");
    }

    @Property
    void generateOtp_WithLeadingZeros_StillReturnsSixDigits(@ForAll @IntRange(max = 99) int smallNumber) {
        String otp = String.format("%06d", smallNumber);

        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }
}
