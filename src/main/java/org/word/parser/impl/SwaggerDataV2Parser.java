package org.word.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.word.model.ModelAttr;
import org.word.model.Request;
import org.word.model.Response;
import org.word.model.Table;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

public class SwaggerDataV2Parser extends AbsSwaggerDataParser {
    public SwaggerDataV2Parser(Map<String, Object> map) {
        super(map);
    }

    @Override
    public String getDefinitionsStr() {
        return "#/definitions/";
    }


    @Override
    protected List<Request> getRequestParamsFromPathConent(Map<String, Object> content) {
        if (content.get("parameters") != null) {
            List<LinkedHashMap> consumes = (List) content.get("parameters");
            return processRequestList(consumes, getDefinitinMap());
        }
        return Lists.newArrayList();
    }

    @Override
    protected String getResponseFormTypeFromPathContent(Map<String, Object> pathContent) {
        List<String> produces = (List) pathContent.get("produces");
        if (produces != null && produces.size() > 0) {
            return StringUtils.join(produces, ",");
        }
        return null;
    }

    @Override
    protected String getRequestFormTypeFromPathContent(Map<String, Object> pathContent) {
        List<String> consumes = (List) pathContent.get("consumes");
        if (!CollectionUtils.isEmpty(consumes)) {
            return StringUtils.join(consumes, ",");
        }
        return null;
    }


    @Override
    protected List<Response> getResponseListFromPathConent(Map<String, Object> pathContent) {
        Map<String, Object> responses = (LinkedHashMap) pathContent.get("responses");
        return processResponseCodeList(responses);
    }


    @Override
    protected ModelAttr getResponseModelAttrFromPathConent(Map<String, Object> pathContent) {
        // 10.返回体
        Map<String, Object> responses = (LinkedHashMap) pathContent.get("responses");

        // 取出来状态是200时的返回值
        Map<String, Object> obj = (Map<String, Object>) responses.get("200");
        if (obj != null && obj.get("schema") != null) {
            return processResponseModelAttrs(obj, definitinMap);
        }
        return null;
    }


    @Override
    public Map<String, Map<String, Object>> getModelsMap(Map<String, Object> map) {
        return (Map<String, Map<String, Object>>) map.get("definitions");
    }

    @Override
    protected String getResponseParamFromPathContent(Map<String, Object> pathContent) throws JsonProcessingException {
        // 10.返回体
        Map<String, Object> responses = (LinkedHashMap) pathContent.get("responses");
        // 取出来状态是200时的返回值
        Map<String, Object> obj = (Map<String, Object>) responses.get("200");
        return processResponseParam(obj, definitinMap);
    }

}
