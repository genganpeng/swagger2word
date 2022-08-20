package org.word.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.word.model.ModelAttr;
import org.word.model.Request;
import org.word.model.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class SwaggerDataV3Parser extends AbsSwaggerDataParser {
    public SwaggerDataV3Parser(Map<String, Object> map) {
        super(map);
    }

    @Override
    public String getDefinitionsStr() {
        return "#/components/schemas/";
    }

    @Override
    public Map<String, Map<String, Object>> getModelsMap(Map<String, Object> map) {
        return (Map<String, Map<String, Object>>) ((Map<String, Object>) map.get("components")).get("schemas");
    }

    @Override
    protected String getResponseParamFromPathContent(Map<String, Object> pathContent) throws JsonProcessingException {
        Map<String, Object> responseMap = (Map<String, Object>) pathContent.get("responses");
        //response content
        Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");
        if (responseContentMap != null) {
            Map<String, Object> responseContentSubMap = (Map<String, Object>) responseContentMap.entrySet().stream().findFirst().get().getValue();
            //处理返回参数
            return processResponseParam(responseContentSubMap, definitinMap);
        }
        return null;
    }

    @Override
    protected ModelAttr getResponseModelAttrFromPathConent(Map<String, Object> pathContent) {
        Map<String, Object> responseMap = (Map<String, Object>) pathContent.get("responses");
        //response content
        Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");
        if (responseContentMap != null) {
            Map<String, Object> responseContentSubMap = (Map<String, Object>) responseContentMap.entrySet().stream().findFirst().get().getValue();
            //处理返回参数
            return processResponseModelAttrs(responseContentSubMap, definitinMap);
        }
        return new ModelAttr();
    }


    private List<Request> processRequestListFromRequestBody(Map<String, Object> requestSchemaMap, Map<String, ModelAttr> definitinMap, boolean requestBodyRequired) {
        List<Request> requestList = new ArrayList<>();
        Object ref = requestSchemaMap.get("$ref");
        if (ref != null) {
            Request request = new Request();
            //参数名称
            request.setName("body");
            //参数类型
            request.setParamType("body");
            //数据类型
            request.setType("object" + ":" + ref.toString().replaceAll(getDefinitionsStr(), ""));
            request.setModelAttr(definitinMap.get(ref));
            // 是否必填
            request.setRequire(requestBodyRequired);
            requestList.add(request);
        }
        return requestList;
    }

    @Override
    protected List<Response> getResponseListFromPathConent(Map<String, Object> pathContent) {
        Map<String, Object> responseMap = (Map<String, Object>) pathContent.get("responses");
        //返回参数列表
        return processResponseCodeList(responseMap);
    }

    @Override
    protected List<Request> getRequestParamsFromPathConent(Map<String, Object> pathContent) {
        List<Request> requests = Lists.newArrayList();
        if (pathContent.get("parameters") != null) {
            List<LinkedHashMap> consumes = (List) pathContent.get("parameters");
            requests.addAll(processRequestList(consumes, getDefinitinMap()));
        }
        if (pathContent.get("requestBody") != null) {
            Map<String, Object> requestBodyMap = (Map<String, Object>) pathContent.get("requestBody");
            Map<String, Object> requestContentMap = (Map<String, Object>) requestBodyMap.get("content");
            if (!CollectionUtils.isEmpty(requestContentMap)) {
                //获取出body中的类
                Map<String, Object> requestSchemaMap = (Map<String, Object>) ((Map<String, Object>) requestContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");
                boolean requestBodyRequired = false;
                if (requestBodyMap.get("required") != null) {
                    requestBodyRequired = (Boolean) requestBodyMap.get("required");
                }
                //请requestBody也添加进行去
                requests.addAll(processRequestListFromRequestBody(requestSchemaMap, definitinMap, requestBodyRequired));
            }
        }
        return requests;
    }

    @Override
    protected String getResponseFormTypeFromPathContent(Map<String, Object> pathContent) {
        Map<String, Object> responseMap = (Map<String, Object>) pathContent.get("responses");
        if (!CollectionUtils.isEmpty(responseMap)) {
            //response content
            Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");
            if (!CollectionUtils.isEmpty(responseContentMap)) {
                //*/*
                return responseContentMap.entrySet().stream().findFirst().get().getKey();
            }
        }
        return null;
    }

    @Override
    protected String getRequestFormTypeFromPathContent(Map<String, Object> pathContent) {
        if (pathContent.get("requestBody") != null) {
            Map<String, Object> requestBodyMap = (Map<String, Object>) pathContent.get("requestBody");
            Map<String, Object> requestContentMap = (Map<String, Object>) requestBodyMap.get("content");
            if (!CollectionUtils.isEmpty(requestContentMap)) {
                //application/json
                return requestContentMap.entrySet().stream().findFirst().get().getKey();
            }
        }
        return null;
    }
}














