package it.gov.pagopa.notifier.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("String: Should mask sensitive fields at root level")
    void maskString_RootLevel() {
        String input = "{\"recipientId\": \"TAX123\", \"visibleField\": \"ok\"}";

        String result = LogUtils.maskSensitiveData(input);

        assertTrue(result.contains("\"recipientId\":\"***\""));
        assertTrue(result.contains("\"visibleField\":\"ok\""));
    }

    @Test
    @DisplayName("String: Should mask sensitive fields nested in objects")
    void maskString_NestedObject() {
        String input = "{\"data\": {\"content\": \"Secret Message\", \"id\": 1}}";

        String result = LogUtils.maskSensitiveData(input);

        assertTrue(result.contains("\"content\":\"***\""));
        assertTrue(result.contains("\"id\":1"));
    }

    @Test
    @DisplayName("String: Should mask sensitive fields inside arrays")
    void maskString_Array() {
        String input = "{\"items\": [{\"fiscalCode\": \"RSSMRA...\"}, {\"fiscalCode\": \"VRDLGI...\"}]}";

        String result = LogUtils.maskSensitiveData(input);

        assertEquals("{\"items\":[{\"fiscalCode\":\"***\"},{\"fiscalCode\":\"***\"}]}", result);
    }

    @Test
    @DisplayName("String: Should return null if input is null")
    void maskString_NullInput() {
        assertNull(LogUtils.maskSensitiveData((String) null));
    }

    @Test
    @DisplayName("String: Should return error message on invalid JSON")
    void maskString_InvalidJson() {
        String invalidJson = "{ \"broken\": ";

        String result = LogUtils.maskSensitiveData(invalidJson);

        assertEquals("(Unparsable Content)", result);
    }

    @Test
    @DisplayName("String: Should minify JSON (remove newlines)")
    void maskString_Minification() {
        String multilineJson = "{\n  \"title\": \"Hello\",\n  \"other\": 123\n}";

        String result = LogUtils.maskSensitiveData(multilineJson);

        assertEquals("{\"title\":\"***\",\"other\":123}", result);
    }

    @Test
    @DisplayName("Object: Should mask Java POJO/DTO")
    void maskObject_Pojo() {
        TestDto dto = new TestDto("SecretTitle", "PublicId");

        String result = LogUtils.maskSensitiveData(dto);

        assertTrue(result.contains("\"title\":\"***\""));
        assertTrue(result.contains("\"publicId\":\"PublicId\""));
    }

    @Test
    @DisplayName("Object: Should mask Java Map")
    void maskObject_Map() {
        Map<String, Object> map = Map.of(
            "recipientId", "SensitiveData",
            "safeKey", "SafeData"
        );

        String result = LogUtils.maskSensitiveData(map);

        assertTrue(result.contains("\"recipientId\":\"***\""));
        assertTrue(result.contains("\"safeKey\":\"SafeData\""));
    }

    @Test
    @DisplayName("Object: Should mask List of Objects")
    void maskObject_List() {
        List<Map<String, String>> list = List.of(
            Map.of("content", "Secret1"),
            Map.of("content", "Secret2")
        );

        String result = LogUtils.maskSensitiveData(list);

        assertEquals("[{\"content\":\"***\"},{\"content\":\"***\"}]", result);
    }

    @Test
    @DisplayName("Object: Should return string 'null' if input is null")
    void maskObject_NullInput() {
        assertEquals("null", LogUtils.maskSensitiveData((Object) null));
    }

    @Test
    @DisplayName("Object: Should handle Serialization Error (Circular Reference)")
    void maskObject_SerializationError() {
        SelfReferencingObject a = new SelfReferencingObject();
        SelfReferencingObject b = new SelfReferencingObject();
        a.setOther(b);
        b.setOther(a); // Loop infinito A -> B -> A

        String result = LogUtils.maskSensitiveData(a);

        assertEquals("(Serialization Error)", result);
    }

    @Data
    @AllArgsConstructor
    static class TestDto {
        private String title;     // Sensitive
        private String publicId;  // Safe
    }

    @Data
    static class SelfReferencingObject {
        private SelfReferencingObject other;
    }
}