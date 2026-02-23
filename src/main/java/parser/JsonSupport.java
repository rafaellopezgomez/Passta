package parser;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

import trace.Trace;


public final class JsonSupport {
    private JsonSupport() {}

    private static final TypeReference<List<Trace>> TRACE_LIST_TYPE = new TypeReference<>() {};

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new BlackbirdModule())
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .build();

    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();
    private static final ObjectReader TRACE_READER = MAPPER.readerFor(TRACE_LIST_TYPE);

    public static ObjectMapper mapper() { return MAPPER; }
    public static ObjectWriter prettyWriter() { return PRETTY; }

    public static ObjectReader tracesReader() {  // 👈 nombre claro
        return TRACE_READER;
    }
}
