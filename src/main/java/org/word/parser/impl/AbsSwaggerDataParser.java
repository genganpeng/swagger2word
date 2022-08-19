package org.word.parser.impl;

import org.word.parser.SwaggerDataParser;

import java.util.Map;

public abstract class AbsSwaggerDataParser implements SwaggerDataParser {
    protected Map<String, Object> map;

    public AbsSwaggerDataParser(Map<String, Object> map) {
        this.map = map;
    }
}
