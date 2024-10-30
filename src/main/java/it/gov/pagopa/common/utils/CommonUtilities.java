package it.gov.pagopa.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.web.exception.EmdEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.function.Consumer;

@Slf4j
public class CommonUtilities {
    private CommonUtilities() {}

    public static final DecimalFormatSymbols decimalFormatterSymbols = new DecimalFormatSymbols();
    public static final DecimalFormat decimalFormatter;

    static {
        decimalFormatterSymbols.setDecimalSeparator(',');
        decimalFormatter = new DecimalFormat("0.00", CommonUtilities.decimalFormatterSymbols);
    }

    /** It will try to deserialize a message, eventually notifying the error  */
    public static <T> T deserializeMessage(Message<?> message, ObjectReader objectReader, Consumer<Throwable> onError) {
        try {
            String payload = readMessagePayload(message);
            return objectReader.readValue(payload);
        } catch (JsonProcessingException e) {
            onError.accept(e);
            return null;
        }
    }

    /** It will read message payload checking if it's a byte[] or String */
    public static String readMessagePayload(Message<?> message) {
        String payload;
        if(message.getPayload() instanceof byte[] bytes){
            payload=new String(bytes);
        } else {
            payload= message.getPayload().toString();
        }
        return payload;
    }

    /** To read Message header value */
        public static Object getHeaderValue(Message<?> message, String headerName) {
        return message.getHeaders().get(headerName);
    }

    /** To read {@link org.apache.kafka.common.header.Header} value */
    public static String getByteArrayHeaderValue(Message<String> message, String headerName) {
        byte[] headerValue = message.getHeaders().get(headerName, byte[].class);
        return headerValue!=null? new String(headerValue, StandardCharsets.UTF_8) : null;
    }

    /** To convert cents into euro */
    public static String createSHA256(String fiscalCode)  {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = md.digest(fiscalCode.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.info("Something went wrong creating SHA256");
            throw new EmdEncryptionException("Something went wrong creating SHA256",true,e);
        }
    }
}
