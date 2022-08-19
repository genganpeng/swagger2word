package org.word.parser.impl;

import org.word.parser.SwaggerDataParser;

import java.util.Map;

public abstract class AbsSwaggerDataParser implements SwaggerDataParser {
    protected Map<String, Object> map;

    public AbsSwaggerDataParser(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * @return 在2.0的api中, 引用变量为：#/definitions/<br>
     * 在3.0的api中, 引用变量为：#/components/schemas/<br>
     */
    public abstract String getDefinitionsStr();
}
