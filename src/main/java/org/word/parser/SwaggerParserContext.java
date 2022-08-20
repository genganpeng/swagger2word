package org.word.parser;

import org.word.model.Table;
import org.word.parser.SwaggerDataParser;
import org.word.parser.impl.SwaggerDataV2Parser;
import org.word.parser.impl.SwaggerDataV3Parser;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwaggerParserContext {
    private SwaggerDataParser swaggerDataParser;

    public SwaggerParserContext(String jsonStr) throws IOException {
        //根据jsonStr获取出version
        Map<String, Object> map = JsonUtils.readValue(jsonStr, HashMap.class);
        if (map.get("openapi") != null) {
            swaggerDataParser = new SwaggerDataV3Parser(map);
        } else {
            swaggerDataParser = new SwaggerDataV2Parser(map);
        }
    }

    public Map<String, Object> doParse(List<Table> result) throws IOException {
        return swaggerDataParser.parse(result);
    }

}
