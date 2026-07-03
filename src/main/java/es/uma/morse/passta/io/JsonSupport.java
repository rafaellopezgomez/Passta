package es.uma.morse.passta.io;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import es.uma.morse.passta.core.trace.Trace;

/**
 * JSON utilities for Trace
 * For very large inputs (GB-scale), prefer the streaming API.
 */
public final class JsonSupport {

    private JsonSupport() {}

    // ----- Types -----
    private static final TypeReference<List<Trace>> TRACE_LIST_TYPE = new TypeReference<>() {};

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new BlackbirdModule())

            // Deserialization settings
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .build();

    // Reader object
    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();
    private static final ObjectReader TRACE_LIST_READER = MAPPER.readerFor(TRACE_LIST_TYPE);
    private static final ObjectReader TRACE_READER = MAPPER.readerFor(Trace.class);

    static ObjectReader traceReader() {
        return TRACE_READER;
    }

    /** Access to the shared ObjectMapper. */
    static ObjectMapper mapper() { return MAPPER; }

    /** Pretty-print writer. */
    static ObjectWriter prettyWriter() { return PRETTY; }

    /**
     * Returns an ObjectReader that reads the root as a List<Trace>.
     * Note: Using this on very large JSON will materialize the entire list in memory.
     */
    static ObjectReader tracesReader() { return TRACE_LIST_READER; }
}





