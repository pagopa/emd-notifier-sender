package it.gov.pagopa.common.utils;



import it.gov.pagopa.common.web.exception.EmdEncryptionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CommonUtilitiesTest {

    @Test
    void createSHA256_Ko_NoSuchAlgorithm() {
        try (MockedStatic<MessageDigest> mockedStatic = Mockito.mockStatic(MessageDigest.class)) {
            mockedStatic.when(() -> MessageDigest.getInstance(any()))
                    .thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));

            EmdEncryptionException exception = assertThrows(EmdEncryptionException.class, () -> CommonUtilities.createSHA256(""));

            assertEquals("SHA-256 not available", exception.getCause().getMessage());
        }
    }
    @Test
    void  createSHA256_Ok(){
        String toHash = "RSSMRA98B18L049O";
        String hashedExpected = "0b393cbe68a39f26b90c80a8dc95abc0fe4c21821195b4671a374c1443f9a1bb";
        String actualHash = CommonUtilities.createSHA256(toHash);
        assertEquals(actualHash,hashedExpected);
    }

}
