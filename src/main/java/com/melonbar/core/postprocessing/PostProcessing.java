package com.melonbar.core.postprocessing;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melonbar.core.http.response.PostProcessor;
import com.melonbar.core.http.response.Response;
import com.melonbar.core.util.Guard;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Static library of common implementations of {@link PostProcessor} and other utility methods.
 */
@Slf4j
public final class PostProcessing {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Convert response body content to an {@link Optional} containing an instance of the desired input {@link Class}.
     * The {@link Optional} will be empty if an error occurred during mapping.
     *
     * @param clazz Mapped object
     * @param <T> Desired type of result wrapped by {@link Optional}
     * @return Instance of {@link T} mapped by {@link Response} content
     */
    public static <T> PostProcessor<Optional<T>> asObject(final Class<T> clazz) {
        return response -> Optional.ofNullable(mapToObject(response.content(), clazz));
    }

    /**
     * Convert response body content to an {@link Optional} containing {@link JsonNode}, where the value is
     * present if the content can be successfully parsed as a json object.
     *
     * @return {@link PostProcessor} that provides <code>Optional{@literal <}JsonNode{@literal >}</code>
     */
    public static PostProcessor<Optional<JsonNode>> asJson() {
        return response -> Optional.ofNullable(marshalJson(response.content()));
    }

    /**
     * Converts the response content into a {@link JsonNode} using <code>asJson()</code> first, then attempts
     * to extract the {@link JsonNode} located at the path <code>key</code>.
     *
     * @param key Json key
     * @return {@link JsonNode} corresponding to json key
     */
    public static PostProcessor<Optional<JsonNode>> getJsonValue(final String key) {
        return response -> asJson()
                .andThen(jsonNode -> jsonNode
                        .map(_jsonNode -> getJsonValue(_jsonNode, key)))
                .apply(response);
    }

    /**
     * Converts the response content into a {@link JsonNode} using <code>asJson()</code> first, then attempts
     * to extract the value as a string using parameter <code>key</code> as a key.
     *
     * @param key Json key
     * @return String value corresponding to json key
     */
    public static PostProcessor<String> getJsonValueAsString(final String key) {
        return response -> getJsonValue(key)
                .andThen(maybeJsonNode -> String.valueOf(maybeJsonNode.orElse(null)))
                .apply(response);
    }

    /**
     * Converts the response content from {@link JsonNode} to a pretty string for debugging/logging purposes.
     *
     * @return Pretty printed json string
     */
    public static PostProcessor<String> asPrettyJsonString() {
        return response -> asJson()
                .andThen(jsonNode -> jsonNode
                        .map(JsonNode::toPrettyString)
                        // empty result
                        .orElse("{}"))
                .apply(response);
    }

    /**
     * Maps input json string to an instance of {@link T} using {@link ObjectMapper}.
     *
     * @param jsonString String in json format
     * @param clazz Class of mapped object
     * @param <T> Desired type of result
     * @return {@link T}, null if failure to map
     */
    private static <T> T mapToObject(final String jsonString, final Class<T> clazz) {
        try {
            return OBJECT_MAPPER.reader().readValue(jsonString, clazz);
        } catch (IOException ioException) {
            log.error("Exception [{}] thrown while attempting to map to [{}] from json: [{}]",
                    ioException.getClass().getName(), clazz.getName(), jsonString);
        }
        return null;
    }

    /**
     * Marshals the input json string into a {@link JsonNode}. Performs "deep" json parsing.
     *
     * @param jsonString String in json format
     * @return {@link JsonNode}
     */
    private static JsonNode marshalJson(final String jsonString) {
        Guard.nonNull(jsonString);
        try {
            return OBJECT_MAPPER.reader()
                    .readTree(OBJECT_MAPPER.getFactory().createParser(jsonString));
        } catch (IOException ioException) {
            log.error("Exception [{}] thrown while attempting to parse content as json: [{}]",
                    ioException.getClass().getName(), jsonString, ioException);
        }
        return null;
    }

    /**
     * Extracts value from {@link JsonNode} provided a path. If the input path refers to just a depth=1 key, then
     * the call defaults to {@link JsonNode#get(String)}.
     *
     * @param jsonNode {@link JsonNode}
     * @param path Path to value
     * @return {@link JsonNode} at the specified path, or {@link com.fasterxml.jackson.databind.node.MissingNode} if
     *  input path is invalid
     */
    private static JsonNode getJsonValue(final JsonNode jsonNode, final String path) {
        return StringUtils.contains(path, JsonPointer.SEPARATOR)
                ? jsonNode.at(path)
                : jsonNode.get(path);
    }
}
