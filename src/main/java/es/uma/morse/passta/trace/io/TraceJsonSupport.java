package es.uma.morse.passta.trace.io;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.module.blackbird.BlackbirdModule;

import es.uma.morse.passta.core.trace.Trace;

/**
 * Internal JSON support for serializing and deserializing traces.
 *
 * <p>For very large inputs, prefer the streaming reader exposed through
 * {@link #traceReader()} instead of materializing a complete list in memory.</p>
 */
final class TraceJsonSupport {

	// ----- Types -----
	private static final TypeReference<List<Trace>> TRACE_LIST_TYPE = new TypeReference<>() {
	};

	private static final ObjectMapper MAPPER = JsonMapper.builder().addModule(new BlackbirdModule())

			 // Detect duplicated JSON properties while parsing.
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            
            // Deserialization settings.
            .enable(
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
            )
            .enable(
                DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY
            )
            .enable(
                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
            )
            .enable(
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS
            )
            .enable(
                DeserializationFeature.FAIL_ON_INVALID_SUBTYPE
            )
            
            
            // Do not accept an integer as the ordinal value of an enum.
            .withCoercionConfig(
                LogicalType.Enum,
                config -> config.setCoercion(
                    CoercionInputShape.Integer,
                    CoercionAction.Fail
                )
            )
            .build();

	private static final ObjectWriter TRACES_WRITER = MAPPER.writerWithDefaultPrettyPrinter();
	private static final ObjectReader TRACES_READER = MAPPER.readerFor(TRACE_LIST_TYPE);
	private static final ObjectReader TRACE_READER = MAPPER.readerFor(Trace.class);
	

	private TraceJsonSupport() {
	}

    /**
     * Returns the shared mapper used for trace JSON.
     */
	static ObjectMapper mapper() {
		return MAPPER;
	}
	

    /**
     * Returns a reader for individual traces.
     *
     * <p>This reader can be used with Jackson's streaming API to process
     * traces without loading the complete input into memory.</p>
     */
	static ObjectReader traceReader() {
		return TRACE_READER;
	}


    /**
     * Returns a pretty-printing writer for trace collections.
     */
	static ObjectWriter tracesWriter() {
		return TRACES_WRITER;
	}

    /**
     * Returns a reader whose root value is a list of traces.
     *
     * <p>This reader materializes the complete list in memory. For very large
     * JSON inputs, use {@link #traceReader()} with the streaming API.</p>
     */
	static ObjectReader tracesReader() {
		return TRACES_READER;
	}
	
	static boolean isJsonFile(Path file) {
		Path fileNamePath = file.getFileName();
		
		if (fileNamePath == null) {
			return false;
		}
		
		String fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);

		return fileName.endsWith(".json");
	}
}
