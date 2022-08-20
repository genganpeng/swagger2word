package org.word.parser.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.word.model.ModelAttr;
import org.word.model.Request;
import org.word.model.Response;
import org.word.model.Table;
import org.word.parser.SwaggerDataParser;
import org.word.utils.JsonUtils;

import java.io.IOException;
import java.util.*;

/**
 * 抽象类，模板方法。封装公共方法
 *
 * @author puhaiyang
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public abstract class AbsSwaggerDataParser implements SwaggerDataParser {
    protected Map<String, Object> map;
    /**
     * 存放每个类的定义信息，类似与spring中的bean
     */
    protected Map<String, ModelAttr> definitinMap = new HashMap<>(256);

    public AbsSwaggerDataParser(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * @return 在2.0的api中, 引用变量为：#/definitions/<br>
     * 在3.0的api中, 引用变量为：#/components/schemas/<br>
     */
    public abstract String getDefinitionsStr();

    /**
     * 解析models
     */
    public Map<String, ModelAttr> parseModels(Map<String, Object> apiMap) {
        Map<String, Map<String, Object>> definitions = getModelsMap(apiMap);
        if (definitions != null) {
            for (String modelName : definitions.keySet()) {
                //遍历解析每一个类
                getAndPutModelAttr(definitions, definitinMap, modelName);
            }
        }
        return definitinMap;
    }

    public Map<String, ModelAttr> getDefinitinMap() {
        return definitinMap;
    }

    /**
     * 递归生成ModelAttr<br>
     * 对$ref类型设置具体属性<br><br>
     * 在2.0的api中,引用变量为：#/definitions/<br>
     * 在3.0的api中, 引用变量为：#/components/schemas/<br>
     *
     * @param modelMetaMap     api中的model定义元数据
     * @param completeModelMap 存放处理好的modelMap
     * @param modelName        要处理的model名称
     */
    private ModelAttr getAndPutModelAttr(Map<String, Map<String, Object>> modelMetaMap, Map<String, ModelAttr> completeModelMap, String modelName) {
        ModelAttr modeAttr;
        if ((modeAttr = completeModelMap.get(getDefinitionsStr() + modelName)) == null) {
            //新对象
            modeAttr = new ModelAttr();
            completeModelMap.put(getDefinitionsStr() + modelName, modeAttr);
        } else if (modeAttr.isCompleted()) {
            //新像已经处理完成了，直接返回
            return completeModelMap.get(getDefinitionsStr() + modelName);
        } else {
            log.debug("[getAndPutModelAttr] modeName:{} is null", modelName);
        }
        //获取出当前处理的modelMap
        Map<String, Object> currentModelMetaMap = modelMetaMap.get(modelName);
        //从元map中获取出属性列表
        Map<String, Object> modeProperties = (Map<String, Object>) currentModelMetaMap.get("properties");
        if (modeProperties == null) {
            log.debug("[getAndPutModelAttr] modeProperties is null.modeName:{}", modelName);
            return null;
        }

        //处理model的属性
        List<ModelAttr> attrList = getModelAttrs(modelMetaMap, completeModelMap, modeAttr, modeProperties);
        List allOf = (List) currentModelMetaMap.get("allOf");
        if (allOf != null) {
            for (int i = 0; i < allOf.size(); i++) {
                Map c = (Map) allOf.get(i);
                if (c.get("$ref") != null) {
                    String refName = c.get("$ref").toString();
                    //截取 引用串 后面的
                    String clsName = refName.substring(getDefinitionsStr().length());
                    Map<String, Object> modeProperties1 = (Map<String, Object>) modelMetaMap.get(clsName).get("properties");
                    //add
                    attrList.addAll(getModelAttrs(modelMetaMap, completeModelMap, modeAttr, modeProperties1));
                }
            }
        }

        Object title = currentModelMetaMap.get("title");
        Object description = currentModelMetaMap.get("description");
        modeAttr.setClassName(title == null ? "" : title.toString());
        modeAttr.setDescription(description == null ? "" : description.toString());
        modeAttr.setProperties(attrList);
        Object required = currentModelMetaMap.get("required");
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


    /**
     * 获取model中的具体参数列表
     *
     * @param modelMetaMap     模型元数据
     * @param completeModelMap 存放处理完成的modelMap
     * @param modeAttr         要处理的model属性
     * @param modelMetaPropMap model的元属性map
     * @return 一个modelName一般由多个字段属性构成，所以这里要返回一个list
     */
    private List<ModelAttr> getModelAttrs(Map<String, Map<String, Object>> modelMetaMap, Map<String, ModelAttr> completeModelMap, ModelAttr modeAttr, Map<String, Object> modelMetaPropMap) {
        Iterator<Map.Entry<String, Object>> mIt = modelMetaPropMap.entrySet().iterator();

        List<ModelAttr> attrList = new ArrayList<>();

        //解析属性
        while (mIt.hasNext()) {
            //对属性一条条遍历
            Map.Entry<String, Object> mEntry = mIt.next();
            //获取属性的value值
            Map<String, Object> attrInfoMap = (Map<String, Object>) mEntry.getValue();
            //一个属性对应一个modelAttr
            ModelAttr child = new ModelAttr();
            //进行属性填充
            child.setName(mEntry.getKey());
            child.setType((String) attrInfoMap.get("type"));
            if (attrInfoMap.get("format") != null) {
                child.setType(child.getType() + "(" + attrInfoMap.get("format") + ")");
            }
            child.setType(StringUtils.defaultIfBlank(child.getType(), "object"));
            //所属类型数据ref的引用类型,会能$ref开头
            Object ref = attrInfoMap.get("$ref");
            Object items = attrInfoMap.get("items");
            if (ref != null || (items != null && (ref = ((Map) items).get("$ref")) != null)) {
                String refName = ref.toString();
                //截取 引用串 后面的,拿到最终的引用名称，即model的名称
                String clsName = refName.substring(getDefinitionsStr().length());
                //标记处理完毕，防止死循环
                modeAttr.setCompleted(true);
                //调用外层的处理方法进行循环处理
                ModelAttr refModel = getAndPutModelAttr(modelMetaMap, completeModelMap, clsName);
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

    public Map<String, Object> parse(List<Table> result) throws IOException {
        //解析model
        Map<String, ModelAttr> definitinMap = parseModels(map);
        //解析paths
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) map.get("paths");
        if (paths != null) {
            for (Map.Entry<String, Map<String, Object>> path : paths.entrySet()) {
                //遍历每一个接口
                // 1.请求路径
                String url = path.getKey();

                // 2. 循环解析每个子节点，适应同一个路径几种请求方式的场景
                for (Map.Entry<String, Object> stringObjectEntry : path.getValue().entrySet()) {
                    //封装Table
                    Table table = new Table();


                    // 2. 请求方式，类似为 get,post,delete,put 这样
                    String requestType = stringObjectEntry.getKey();

                    Map<String, Object> content = (Map<String, Object>) stringObjectEntry.getValue();
                    // 4. 大标题（类说明）
                    String title = String.valueOf(((List) content.get("tags")).get(0));

                    // 5.小标题 （方法说明）
                    String tag = String.valueOf(content.get("summary"));

                    // 6.接口描述
                    String description = String.valueOf(content.get("summary"));
                    // 7.请求参数格式，类似于 multipart/form-data
                    String requestFormType = getRequestFormTypeFromPathContent(content);
//                    Map<String, Object> requestBodyMap = null;
//                    Map<String, Object> requestContentMap = null;
//                    if (content.get("requestBody") != null) {
//                        requestBodyMap = (Map<String, Object>) content.get("requestBody");
//                        requestContentMap = (Map<String, Object>) requestBodyMap.get("content");
//                        if (!CollectionUtils.isEmpty(requestContentMap)) {
//                            //application/json
//                            requestForm = requestContentMap.entrySet().stream().findFirst().get().getKey();
//                            //获取出body中的类
//                            Map<String, Object> requestSchemaMap = (Map<String, Object>) ((Map<String, Object>) requestContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");
//                            boolean requestBodyRequired = false;
//                            if (requestBodyMap.get("required") != null) {
//                                requestBodyRequired = (Boolean) requestBodyMap.get("required");
//                            }
//                            //请requestBody也添加进行去
//                            requestParamList.addAll(processRequestListFromRequestBody(requestSchemaMap, definitinMap, requestBodyRequired));
//                        }
//                    }

                    //请求参数列表
                    List<Request> requestParamList = getRequestParamsFromPathConent(content);
//                    if (content.get("parameters") != null) {
//                        List<LinkedHashMap> consumes = (List) content.get("parameters");
//                        requestParamList = processRequestList(consumes, definitinMap);
//                    }


//                    if (requestContentMap != null && !requestContentMap.entrySet().isEmpty()) {
//                        //application/json
//                        requestForm = requestContentMap.entrySet().stream().findFirst().get().getKey();
//                        //获取出body中的类
//                        Map<String, Object> requestSchemaMap = (Map<String, Object>) ((Map<String, Object>) requestContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");
//                        boolean requestBodyRequired = false;
//                        if (requestBodyMap.get("required") != null) {
//                            requestBodyRequired = (Boolean) requestBodyMap.get("required");
//                        }
//                        //请requestBody也添加进行去
//                        requestParamList.addAll(processRequestListFromRequestBody(requestSchemaMap, definitinMap, requestBodyRequired));
//                    }


                    // 8.返回参数格式，类似于 application/json
                    String responseFormType = getResponseFormTypeFromPathContent(content);
//                    Map<String, Object> responseMap = (Map<String, Object>) content.get("responses");
//                    //返回参数列表
//                    List<Response> responseList = processResponseCodeList(responseMap);
                    List<Response> responseList = getResponseListFromPathConent(content);
//                    //response content
//                    Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");
//
//                    String responseParam = null;
//                    //*/*
//                    if (responseContentMap != null) {
//                        //*/*
//                        responseForm = responseContentMap.entrySet().stream().findFirst().get().getKey();
//                        //
//                        Map<String, Object> responseContentSubMap = (Map<String, Object>) responseContentMap.entrySet().stream().findFirst().get().getValue();
//                        //处理返回参数
//                        table.setModelAttr(processResponseModelAttrs(responseContentSubMap, definitinMap));
//                        //返回示例
//                        responseParam = processResponseParam(responseContentSubMap, definitinMap);
//                    } else if (((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("response") != null) {
//
//                    }

                    table.setTitle(title);
                    table.setUrl(url);
                    table.setTag(tag);
                    table.setRequestType(requestType);
                    table.setDescription(description);
                    table.setRequestForm(requestFormType);
                    table.setResponseForm(responseFormType);
                    //请求体处理
                    table.setRequestList(requestParamList);
                    //响应码处理
                    table.setResponseList(responseList);
                    //返回值列表
                    table.setModelAttr(getResponseModelAttrFromPathConent(content));
                    //示例
                    table.setRequestParam(processRequestParam(table.getRequestList()));
                    table.setResponseParam(getResponseParamFromPathContent(content));

                    result.add(table);
                }

            }
        }

        return map;
    }


    /**
     * 处理返回码列表
     *
     * @param responses 全部状态码返回对象
     */
    protected List<Response> processResponseCodeList(Map<String, Object> responses) {
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

    /**
     * 处理返回属性列表
     */
    protected ModelAttr processResponseModelAttrs(Map<String, Object> responseObj, Map<String, ModelAttr> definitinMap) {
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

    protected abstract String getResponseParamFromPathContent(Map<String, Object> pathContent) throws JsonProcessingException;

    protected abstract ModelAttr getResponseModelAttrFromPathConent(Map<String, Object> pathContent);

    /**
     * 例子中，字段的默认值
     *
     * @param type      类型
     * @param modelAttr 引用的类型
     * @return
     */
    private Object getValue(String type, ModelAttr modelAttr) {
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
     * 处理返回值
     */
    protected String processResponseParam(Map<String, Object> responseObj, Map<String, ModelAttr> definitinMap) throws JsonProcessingException {
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
     * 获取响应列表
     *
     * @param pathContent pathContent
     * @return List<Response>
     */
    protected abstract List<Response> getResponseListFromPathConent(Map<String, Object> pathContent);

    /**
     * 获取出请求参数列表
     *
     * @param pathContent pathContent
     * @return List<Request>
     */
    protected abstract List<Request> getRequestParamsFromPathConent(Map<String, Object> pathContent);

    /**
     * 获取出响应form类型
     *
     * @param pathContent pathContent
     * @return 响应表单类型，类似于 application/json
     */
    protected abstract String getResponseFormTypeFromPathContent(Map<String, Object> pathContent);

    /**
     * 获取出请求form类型
     *
     * @param pathContent pathContent
     * @return 请求表单类型，类似于 multipart/form-data
     */
    protected abstract String getRequestFormTypeFromPathContent(Map<String, Object> pathContent);


    /**
     * 处理请求参数列表
     */
    protected List<Request> processRequestList(List<LinkedHashMap> parameters, Map<String, ModelAttr> definitinMap) {
        List<Request> requestList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parameters)) {
            for (Map<String, Object> param : parameters) {
                Object in = param.get("in");
                Request request = new Request();
                request.setName(String.valueOf(param.get("name")));
                request.setType(param.get("type") == null ? "object" : param.get("type").toString());
                Map<String, Object> subSchemaMap = (Map<String, Object>) param.get("schema");
                if (subSchemaMap != null) {
                    request.setType(subSchemaMap.get("type") == null ? "object" : subSchemaMap.get("type").toString());
                }
                if (param.get("format") != null) {
                    request.setType(request.getType() + "(" + param.get("format") + ")");
                }
                request.setParamType(String.valueOf(in));
                // 考虑对象参数类型
                if ("body".equals(in)) {
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
                Object value = getExampleValue(request.getType(), request.getModelAttr());
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
     * 例子中，字段的默认值
     *
     * @param type      类型
     * @param modelAttr 引用的类型
     */
    private Object getExampleValue(String type, ModelAttr modelAttr) {
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
                        map.put(subModelAttr.getName(), getExampleValue(subModelAttr.getType(), subModelAttr));
                    }
                }
                list.add(map);
                return list;
            case "object":
                map = new LinkedHashMap<>();
                if (modelAttr != null && !CollectionUtils.isEmpty(modelAttr.getProperties())) {
                    for (ModelAttr subModelAttr : modelAttr.getProperties()) {
                        map.put(subModelAttr.getName(), getExampleValue(subModelAttr.getType(), subModelAttr));
                    }
                }
                return map;
            default:
                return null;
        }
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
     * 获取出model的map
     *
     * @param map apiMap
     * @return map<model的名称, model的map>
     */
    public abstract Map<String, Map<String, Object>> getModelsMap(Map<String, Object> map);
}
