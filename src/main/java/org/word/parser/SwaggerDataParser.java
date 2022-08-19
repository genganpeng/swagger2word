package org.word.parser;

import org.word.model.Table;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface SwaggerDataParser {

    Map<String, Object> parse(List<Table> resultx) throws IOException;
}
