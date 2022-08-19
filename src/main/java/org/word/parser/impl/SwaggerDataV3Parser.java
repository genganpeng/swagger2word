package org.word.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.word.model.ModelAttr;
import org.word.model.Request;
import org.word.model.Response;
import org.word.model.Table;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

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
    public Map<String, Object> parse(List<Table> result) throws IOException {
        //解析model
        Map<String, ModelAttr> definitinMap = parseDefinitions(map);
        //解析paths
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) map.get("paths");
        if (paths != null) {
            Iterator<Map.Entry<String, Map<String, Object>>> it = paths.entrySet().iterator();
            while (it.hasNext()) {
                //遍历每一个接口
                Map.Entry<String, Map<String, Object>> path = it.next();
                // 1.请求路径
                String url = path.getKey();

                Iterator<Map.Entry<String, Object>> it2 = path.getValue().entrySet().iterator();

                // 2. 循环解析每个子节点，适应同一个路径几种请求方式的场景
                while (it2.hasNext()) {
                    //封装Table
                    Table table = new Table();
                    //请求参数列表
                    List<Request> requestParamList = new ArrayList<>();

                    Map.Entry<String, Object> request = it2.next();
                    // 2. 请求方式，类似为 get,post,delete,put 这样
                    String requestType = request.getKey();

                    Map<String, Object> content = (Map<String, Object>) request.getValue();
                    // 4. 大标题（类说明）
                    String title = String.valueOf(((List) content.get("tags")).get(0));

                    // 5.小标题 （方法说明）
                    String tag = String.valueOf(content.get("summary"));

                    // 6.接口描述
                    String description = String.valueOf(content.get("summary"));
                    // 7.请求参数格式，类似于 multipart/form-data
                    String requestForm = "";
                    Map<String, Object> requestBodyMap = null;
                    if (content.get("requestBody") != null) {
                        requestBodyMap = (Map<String, Object>) content.get("requestBody");
                    } else if (content.get("parameters") != null) {
                        List<LinkedHashMap> consumes = (List) content.get("parameters");
                        requestParamList = processRequestList(consumes, definitinMap);
                    }

                    Map<String, Object> requestContentMap = null;
                    if (requestBodyMap != null && requestBodyMap.get("content") != null) {
                        requestContentMap = (Map<String, Object>) requestBodyMap.get("content");
                    } else if (content.get("parameters") != null) {
                        List<String> consumes = (List) content.get("parameters");
                    }


                    if (requestContentMap != null && !requestContentMap.entrySet().isEmpty()) {
                        //application/json
                        requestForm = requestContentMap.entrySet().stream().findFirst().get().getKey();
                        //获取出body中的类
                        Map<String, Object> requestSchemaMap = (Map<String, Object>) ((Map<String, Object>) requestContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");
                        //请求参数列表
//                        List<Request> requests = processRequestList(requestSchemaMap, definitinMap);
                        boolean requestBodyRequired = false;
                        if (requestBodyMap.get("required") != null) {
                            requestBodyRequired = (Boolean) requestBodyMap.get("required");
                        }
                        //请requestBody也添加进行去
                        requestParamList.addAll(processRequestListFromRequestBody(requestSchemaMap, definitinMap, requestBodyRequired));
                    }


                    // 8.返回参数格式，类似于 application/json
                    String responseForm = "";
                    Map<String, Object> responseMap = (Map<String, Object>) content.get("responses");
                    //返回参数列表
                    List<Response> responseList = processResponseCodeList(responseMap);
                    // 取出来状态是200时的返回值
//                    Map<String, Object> obj = (Map<String, Object>) responseMap.get("200");
//                    if (obj != null && obj.get("content") != null) {
//                        //传入的是有schema的那个map
//                        table.setModelAttr(processResponseModelAttrs(obj, definitinMap));
//                    }

                    //response content
                    Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");

                    String responseParam = null;
                    //*/*
                    if (responseContentMap != null) {
                        //*/*
                        responseForm = responseContentMap.entrySet().stream().findFirst().get().getKey();
                        //
                        Map<String, Object> responseContentSubMap = (Map<String, Object>) responseContentMap.entrySet().stream().findFirst().get().getValue();
                        //处理返回参数
                        table.setModelAttr(processResponseModelAttrs(responseContentSubMap, definitinMap));
                        //返回示例
                        responseParam = processResponseParam(responseContentSubMap, definitinMap);
                    } else if (((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("response") != null) {

                    }


                    table.setTitle(title);
                    table.setUrl(url);
                    table.setTag(tag);
                    table.setDescription(description);
                    table.setRequestForm(requestForm);
                    table.setResponseForm(responseForm);
                    table.setRequestType(requestType);
                    //请求体处理
                    table.setRequestList(requestParamList);
                    //响应码处理
                    table.setResponseList(responseList);

                    //示例
                    table.setRequestParam(processRequestParam(table.getRequestList()));
                    table.setResponseParam(responseParam);

                    result.add(table);
                }

            }
        }

        return map;
    }


    /**
     * 处理返回值
     */
    private String processResponseParam(Map<String, Object> responseObj, Map<String, ModelAttr> definitinMap) throws JsonProcessingException {
        if (responseObj != null && responseObj.get("schema") != null) {
            Map<String, Object> schema = (Map<String, Object>) responseObj.get("schema");
            String type = (String) schema.get("type");
            String ref = null;
            // 数组
            if ("array".equals(type)) {
                Map<String, Object> items = (Map<String, Object>) schema.get("items");
                if (items != null && items.get("$ref") != null) {
                    ref = (String) items.get("$ref");
                }
            }
            // 对象
            if (schema.get("$ref") != null) {
                ref = (String) schema.get("$ref");
            }
            if (StringUtils.isNotEmpty(ref)) {
                ModelAttr modelAttr = definitinMap.get(ref);
                if (modelAttr != null && !CollectionUtils.isEmpty(modelAttr.getProperties())) {
                    Map<String, Object> responseMap = new HashMap<>(8);
                    for (ModelAttr subModelAttr : modelAttr.getProperties()) {
                        responseMap.put(subModelAttr.getName(), getValue(subModelAttr.getType(), subModelAttr));
                    }
                    return JsonUtils.writeJsonStr(responseMap);
                }
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * 例子中，字段的默认值
     *
     * @param type      类型
     * @param modelAttr 引用的类型
     * @return
     */
    private Object getValue(String type, ModelAttr modelAttr) {
        if (type == null) {
            return null;
        }
        int pos;
        if ((pos = type.indexOf(":")) != -1) {
            type = type.substring(0, pos);
        }
        switch (type) {
            case "string":
                return "string";
            case "string(date-time)":
                return "2020/01/01 00:00:00";
            case "integer":
            case "integer(int64)":
            case "integer(int32)":
                return 0;
            case "number":
                return 0.0;
            case "boolean":
                return true;
            case "file":
                return "(binary)";
            case "array":
                List list = new ArrayList();
                Map<String, Object> map = new LinkedHashMap<>();
                if (modelAttr != null && !CollectionUtils.isEmpty(modelAttr.getProperties())) {
                    for (ModelAttr subModelAttr : modelAttr.getProperties()) {
                        map.put(subModelAttr.getName(), getValue(subModelAttr.getType(), subModelAttr));
                    }
                }
                list.add(map);
                return list;
            case "object":
                map = new LinkedHashMap<>();
                if (modelAttr != null && !CollectionUtils.isEmpty(modelAttr.getProperties())) {
                    for (ModelAttr subModelAttr : modelAttr.getProperties()) {
                        map.put(subModelAttr.getName(), getValue(subModelAttr.getType(), subModelAttr));
                    }
                }
                return map;
            default:
                return null;
        }
    }

    /**
     * 封装请求体
     *
     * @param list
     * @return
     */
    private String processRequestParam(List<Request> list) throws IOException {
        Map<String, Object> headerMap = new LinkedHashMap<>();
        Map<String, Object> queryMap = new LinkedHashMap<>();
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        if (list != null && list.size() > 0) {
            for (Request request : list) {
                String name = request.getName();
                String paramType = request.getParamType();
                Object value = getValue(request.getType(), request.getModelAttr());
                switch (paramType) {
                    case "header":
                        headerMap.put(name, value);
                        break;
                    case "query":
                        queryMap.put(name, value);
                        break;
                    case "body":
                        //TODO 根据content-type序列化成不同格式，目前只用了json
                        jsonMap.put(name, value);
                        break;
                    default:
                        break;

                }
            }
        }
        String res = "";
        if (!queryMap.isEmpty()) {
            res += getUrlParamsByMap(queryMap);
        }
        if (!headerMap.isEmpty()) {
            res += " " + getHeaderByMap(headerMap);
        }
        if (!jsonMap.isEmpty()) {
            if (jsonMap.size() == 1) {
                for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                    res += " -d '" + JsonUtils.writeJsonStr(entry.getValue()) + "'";
                }
            } else {
                res += " -d '" + JsonUtils.writeJsonStr(jsonMap) + "'";
            }
        }
        return res;
    }


    /**
     * 将map转换成header
     */
    public static String getHeaderByMap(Map<String, Object> map) {
        if (CollectionUtils.isEmpty(map)) {
            return "";
        }
        StringBuilder sBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sBuilder.append("--header '");
            sBuilder.append(entry.getKey() + ":" + entry.getValue());
            sBuilder.append("'");
        }
        return sBuilder.toString();
    }

    public static String getUrlParamsByMap(Map<String, Object> map) {
        if (CollectionUtils.isEmpty(map)) {
            return "";
        }
        StringBuilder sBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sBuilder.append(entry.getKey() + "=" + entry.getValue());
            sBuilder.append("&");
        }
        String s = sBuilder.toString();
        if (s.endsWith("&")) {
            s = StringUtils.substringBeforeLast(s, "&");
        }
        return s;
    }

    /**
     * 处理返回属性列表
     */
    private ModelAttr processResponseModelAttrs(Map<String, Object> responseObj, Map<String, ModelAttr> definitinMap) {
        Map<String, Object> schema = (Map<String, Object>) responseObj.get("schema");
        String type = (String) schema.get("type");
        String ref = null;
        //数组
        if ("array".equals(type)) {
            Map<String, Object> items = (Map<String, Object>) schema.get("items");
            if (items != null && items.get("$ref") != null) {
                ref = (String) items.get("$ref");
            }
        }
        //对象
        if (schema.get("$ref") != null) {
            ref = (String) schema.get("$ref");
        }

        //其他类型
        ModelAttr modelAttr = new ModelAttr();
        modelAttr.setType(StringUtils.defaultIfBlank(type, StringUtils.EMPTY));

        if (StringUtils.isNotBlank(ref) && definitinMap.get(ref) != null) {
            modelAttr = definitinMap.get(ref);
        }
        return modelAttr;
    }

    private List<Response> processResponseCodeListByResponseMap(Map<String, Object> responseMap) {
        List<Response> responseList = new ArrayList<>();
        return responseList;
    }


    /**
     * 处理返回码列表
     *
     * @param responses 全部状态码返回对象
     */
    private List<Response> processResponseCodeList(Map<String, Object> responses) {
        List<Response> responseList = new ArrayList<>();
        Iterator<Map.Entry<String, Object>> resIt = responses.entrySet().iterator();
        while (resIt.hasNext()) {
            Map.Entry<String, Object> entry = resIt.next();
            Response response = new Response();
            // 状态码 200 201 401 403 404 这样
            response.setName(entry.getKey());
            LinkedHashMap<String, Object> statusCodeInfo = (LinkedHashMap) entry.getValue();
            response.setDescription(String.valueOf(statusCodeInfo.get("description")));
            Object schema = statusCodeInfo.get("schema");
            if (schema != null) {
                Object originalRef = ((LinkedHashMap) schema).get("originalRef");
                response.setRemark(originalRef == null ? "" : originalRef.toString());
            }
            responseList.add(response);
        }
        return responseList;
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

    /**
     * 处理请求参数列表
     */
    private List<Request> processRequestList(List<LinkedHashMap> parameters, Map<String, ModelAttr> definitinMap) {
        List<Request> requestList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parameters)) {
            for (Map<String, Object> param : parameters) {
                Object in = param.get("in");
                Request request = new Request();
                request.setName(String.valueOf(param.get("name")));
                request.setType(param.get("type") == null ? "object" : param.get("type").toString());
                if (param.get("format") != null) {
                    request.setType(request.getType() + "(" + param.get("format") + ")");
                }
                request.setParamType(String.valueOf(in));
                // 考虑对象参数类型
                if (in != null && "body".equals(in)) {
                    request.setType(String.valueOf(in));
                    Map<String, Object> schema = (Map) param.get("schema");
                    Object ref = schema.get("$ref");
                    // 数组情况另外处理
                    if (schema.get("type") != null && "array".equals(schema.get("type"))) {
                        ref = ((Map) schema.get("items")).get("$ref");
                        request.setType("array");
                    }
                    if (ref != null) {
                        request.setType(request.getType() + ":" + ref.toString().replaceAll(getDefinitionsStr(), ""));
                        request.setModelAttr(definitinMap.get(ref));
                    }
                }
                // 是否必填
                request.setRequire(false);
                if (param.get("required") != null) {
                    request.setRequire((Boolean) param.get("required"));
                }
                // 参数说明
                request.setRemark(String.valueOf(param.get("description")));
                requestList.add(request);
            }
        }
        return requestList;
    }

    private Map<String, ModelAttr> parseDefinitions(Map<String, Object> map) {
        Map<String, Map<String, Object>> definitions = (Map<String, Map<String, Object>>) ((Map<String, Object>) map.get("components")).get("schemas");
        //存放每个类的定义信息，类似与spring中的bean
        Map<String, ModelAttr> definitinMap = new HashMap<>(256);
        if (definitions != null) {
            Iterator<String> modelNameIt = definitions.keySet().iterator();
            while (modelNameIt.hasNext()) {
                //遍历解析每一个类
                String modeName = modelNameIt.next();
                getAndPutModelAttr(definitions, definitinMap, modeName);
            }
        }
        return definitinMap;
    }


    /**
     * 递归生成ModelAttr<br>
     * 对$ref类型设置具体属性<br><br>
     * 在2.0的api中,引用变量为：#/definitions/<br>
     * 在3.0的api中, 引用变量为：#/components/schemas/<br>
     *
     * @param resMap 存放定义好的map <br><br><br>
     */
    private ModelAttr getAndPutModelAttr(Map<String, Map<String, Object>> swaggerMap, Map<String, ModelAttr> resMap, String modeName) {
        ModelAttr modeAttr;
        if ((modeAttr = resMap.get(getDefinitionsStr() + modeName)) == null) {
            //新对象
            modeAttr = new ModelAttr();
            resMap.put(getDefinitionsStr() + modeName, modeAttr);
        } else if (modeAttr.isCompleted()) {
            //新像已经处理完成了，直接返回
            return resMap.get(getDefinitionsStr() + modeName);
        } else {
//            log.error("[getAndPutModelAttr] modeName:{} is null", modeName);
        }

        //从原map中获取出属性列表
        Map<String, Object> modeProperties = (Map<String, Object>) swaggerMap.get(modeName).get("properties");
        if (modeProperties == null) {
            return null;
        }

        List<ModelAttr> attrList = getModelAttrs(swaggerMap, resMap, modeAttr, modeProperties);
        List allOf = (List) swaggerMap.get(modeName).get("allOf");
        if (allOf != null) {
            for (int i = 0; i < allOf.size(); i++) {
                Map c = (Map) allOf.get(i);
                if (c.get("$ref") != null) {
                    String refName = c.get("$ref").toString();
                    //截取 引用串 后面的
                    String clsName = refName.substring(getDefinitionsStr().length());
                    Map<String, Object> modeProperties1 = (Map<String, Object>) swaggerMap.get(clsName).get("properties");
                    List<ModelAttr> attrList1 = getModelAttrs(swaggerMap, resMap, modeAttr, modeProperties1);
                    if (attrList1 != null && attrList != null) {
                        attrList.addAll(attrList1);
                    } else if (attrList == null && attrList1 != null) {
                        attrList = attrList1;
                    }
                }
            }
        }

        Object title = swaggerMap.get(modeName).get("title");
        Object description = swaggerMap.get(modeName).get("description");
        modeAttr.setClassName(title == null ? "" : title.toString());
        modeAttr.setDescription(description == null ? "" : description.toString());
        modeAttr.setProperties(attrList);
        Object required = swaggerMap.get(modeName).get("required");
        if (Objects.nonNull(required)) {
            if ((required instanceof List) && !CollectionUtils.isEmpty(attrList)) {
                List requiredList = (List) required;
                attrList.stream().filter(m -> requiredList.contains(m.getName())).forEach(m -> m.setRequire(true));
            } else if (required instanceof Boolean) {
                modeAttr.setRequire(Boolean.parseBoolean(required.toString()));
            }
        }
        return modeAttr;
    }


    private List<ModelAttr> getModelAttrs(Map<String, Map<String, Object>> swaggerMap, Map<String, ModelAttr> resMap, ModelAttr modeAttr, Map<String, Object> modeProperties) {
        Iterator<Map.Entry<String, Object>> mIt = modeProperties.entrySet().iterator();

        List<ModelAttr> attrList = new ArrayList<>();

        //解析属性
        while (mIt.hasNext()) {
            Map.Entry<String, Object> mEntry = mIt.next();
            Map<String, Object> attrInfoMap = (Map<String, Object>) mEntry.getValue();
            ModelAttr child = new ModelAttr();
            child.setName(mEntry.getKey());
            child.setType((String) attrInfoMap.get("type"));
            if (attrInfoMap.get("format") != null) {
                child.setType(child.getType() + "(" + attrInfoMap.get("format") + ")");
            }
            child.setType(StringUtils.defaultIfBlank(child.getType(), "object"));

            Object ref = attrInfoMap.get("$ref");
            Object items = attrInfoMap.get("items");
            if (ref != null || (items != null && (ref = ((Map) items).get("$ref")) != null)) {
                String refName = ref.toString();
                //截取 引用串 后面的
                String clsName = refName.substring(getDefinitionsStr().length());
                modeAttr.setCompleted(true);
                ModelAttr refModel = getAndPutModelAttr(swaggerMap, resMap, clsName);
                if (refModel != null) {
                    child.setProperties(refModel.getProperties());
                }
                child.setType(child.getType() + ":" + clsName);
            }
            child.setDescription((String) attrInfoMap.get("description"));
            attrList.add(child);
        }
        return attrList;
    }

}














