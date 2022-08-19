package org.word.parser.impl;

import org.word.model.Table;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SwaggerDataV3Parser extends AbsSwaggerDataParser {
    public SwaggerDataV3Parser(Map<String, Object> map) {
        super(map);
    }

    @Override
    public Map<String, Object> parse(List<Table> result) throws IOException {
        return null;
    }
}
