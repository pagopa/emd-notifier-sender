package it.gov.pagopa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.web.exception.EmdEncryptionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommonUtilitiesTest {

    @Mock
    private Message<String> messageMock;
    @Mock
    private ObjectReader objectReaderMock;

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
    void createSHA256_Ok() {
        String toHash = "RSSMRA98B18L049O";
        String hashedExpected = "0b393cbe68a39f26b90c80a8dc95abc0fe4c21821195b4671a374c1443f9a1bb";
        String actualHash = CommonUtilities.createSHA256(toHash);
        assertEquals(hashedExpected, actualHash);
    }

    @Test
    void deserializeMessage_Ko_JsonProcessingException() {
        Consumer<Throwable> errorHandler = e -> Assertions.assertTrue(e instanceof JsonProcessingException);
        when(messageMock.getPayload()).thenReturn("invalid payload");

        try {
            when(objectReaderMock.readValue("invalid payload")).thenThrow(new JsonProcessingException("Invalid JSON") {});

            // Act
            Object result = CommonUtilities.deserializeMessage(messageMock, objectReaderMock, errorHandler);

            // Assert
            Assertions.assertNull(result);
        } catch (JsonProcessingException e) {
            Assertions.fail("Exception should be handled in the method");
        }
    }

    @Test
    void deserializeMessage_Ok() {
        // Setup
        String validJson = "{\"name\":\"John\"}";
        MyObject expectedObject = new MyObject("John");
        Consumer<Throwable> errorHandler = e -> Assertions.fail("Should not have thrown an error");

        when(messageMock.getPayload()).thenReturn(validJson);
        try {
            when(objectReaderMock.readValue(validJson)).thenReturn(expectedObject);

            MyObject result = CommonUtilities.deserializeMessage(messageMock, objectReaderMock, errorHandler);

            Assertions.assertNotNull(result);
            assertEquals(expectedObject.name(), result.name());
        } catch (JsonProcessingException e) {
            Assertions.fail("Exception should not be thrown");
        }
    }

    @Test
    void readMessagePayload_StringPayload() {
        String expectedPayload = "test message";
        when(messageMock.getPayload()).thenReturn(expectedPayload);

        String actualPayload = CommonUtilities.readMessagePayload(messageMock);

        assertEquals(expectedPayload, actualPayload);
    }

    @Test
    void getHeaderValue_Test() {
        String headerName = "testHeader";
        String headerValue = "headerValue";
        MessageHeaders headers = new MessageHeaders(Map.of(headerName, headerValue));

        when(messageMock.getHeaders()).thenReturn(headers);

        Object result = CommonUtilities.getHeaderValue(messageMock, headerName);

        assertEquals(headerValue, result);
    }

    @Test
    void getByteArrayHeaderValue_Test() {
        String headerName = "byteArrayHeader";
        String headerValue = "headerValue";
        MessageHeaders headers = new MessageHeaders(Map.of(headerName, headerValue.getBytes(StandardCharsets.UTF_8)));
        when(messageMock.getHeaders()).thenReturn(headers);

        String result = CommonUtilities.getByteArrayHeaderValue(messageMock, headerName);

        assertEquals(headerValue, result);
    }
        record MyObject(String name) { }
}
