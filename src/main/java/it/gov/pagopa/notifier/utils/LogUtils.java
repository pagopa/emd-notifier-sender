package it.gov.pagopa.notifier.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Utility class designed to mask sensitive data within JSON structures to ensure secure logging.
 * <p>
 * This class uses Jackson to traverse JSON trees (recursively) and replace the values of specific
 * sensitive fields (defined in {@link #SENSITIVE_FIELDS}) with a placeholder ("***").
 * It supports both raw JSON strings and Java objects as input.
 * </p>
 */
@Slf4j
public class LogUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Set of field names that contain sensitive PII (Personally Identifiable Information)
     * or security data. These fields will be redacted in the output.
     */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "recipientId", "content", "title", "fiscalCode"
    );

    /**
     * Parses a raw JSON string, masks sensitive fields, and returns the result as a minified JSON string.
     * <p>
     * This method is fail-safe: if the input is not valid JSON, it returns a placeholder error message
     * instead of throwing an exception, ensuring the logging flow is not interrupted.
     * </p>
     *
     * @param jsonInput The raw JSON string to be masked.
     * @return A masked, minified JSON string, {@code null} if the input is null,
     * or "(Unparsable Content)" if parsing fails.
     */
    public static String maskSensitiveData(String jsonInput) {
        if (jsonInput == null) return null;
        try {
            JsonNode root = mapper.readTree(jsonInput);
            maskNode(root);
            return root.toString();
        } catch (Exception e) {
            return "(Unparsable Content)";
        }
    }

    /**
     * Serializes a Java object into JSON, masks sensitive fields, and returns the result
     * as a minified JSON string.
     * <p>
     * This eliminates the need to manually serialize objects using an ObjectMapper
     * before logging them.
     * </p>
     *
     * @param object The Java object (DTO, Entity, Map, etc.) to be masked.
     * @return A masked, minified JSON string representation of the object,
     * "null" string if the input is null, or "(Serialization Error)" if serialization fails.
     */
    public static String maskSensitiveData(Object object) {
        if (object == null) return "null";
        try {
            JsonNode root = mapper.valueToTree(object);
            maskNode(root);
            return root.toString();
        } catch (Exception e) {
            return "(Serialization Error)";
        }
    }

    /**
     * Recursively traverses the JSON node tree to find and mask sensitive fields.
     * * @param node The current JSON node being processed (Object, Array, or Value).
     */
    private static void maskNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                if (SENSITIVE_FIELDS.contains(fieldName)) {
                    // Replace the sensitive value with asterisks
                    objectNode.set(fieldName, new TextNode("***"));
                } else {
                    // Recurse into the child node (to handle nested objects)
                    maskNode(objectNode.get(fieldName));
                }
            });
        } else if (node.isArray()) {
            // Iterate over array elements and recurse
            node.forEach(LogUtils::maskNode);
        }
    }
}