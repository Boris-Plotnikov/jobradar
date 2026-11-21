package dev.plotnikov.jobradar.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

final class JsonValues {
    private JsonValues() {}

    static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        try {
            return (int) Double.parseDouble(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static List<String> strings(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        JsonNode value = node.get(field);
        if (value != null && value.isArray()) {
            value.forEach(item -> result.add(item.asText()));
        }
        return result;
    }
}
