package org.word.parser.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.word.model.ModelAttr;
import org.word.model.Request;
import org.word.model.Table;

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
                        //TODO 请求参数处理逻辑；请求参数和请求body同时存在的情况下
                        List<String> consumes = (List) content.get("parameters");
                        log.error("[todo] process paramters.");
                    }

                    Map<String, Object> requestContentMap = null;
                    if (requestBodyMap != null && requestBodyMap.get("content") != null) {
                        requestContentMap = (Map<String, Object>) requestBodyMap.get("content");
                    } else if (content.get("parameters") != null) {
                        List<String> consumes = (List) content.get("parameters");
                    }
                    if (requestContentMap != null && !requestContentMap.entrySet().isEmpty()) {
                        //application/json
                        requestForm = StringUtils.join(requestContentMap.entrySet(), ",");
                        //获取出body中的类
                        Map<String, Object> requestSchemaMap = (Map<String, Object>) ((Map<String, Object>) requestContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");
                    }


                    // 8.返回参数格式，类似于 application/json
                    String responseForm = "";
                    Map<String, Object> responseMap = (Map<String, Object>) content.get("responses");
                    //200
                    String responseStateCode = responseMap.entrySet().stream().findFirst().get().getKey();
                    //response content
                    Map<String, Object> responseContentMap = (Map<String, Object>) ((Map<String, Object>) (responseMap.entrySet().stream().findFirst().get().getValue())).get("content");
                    //*/*
                    responseForm = responseContentMap.entrySet().stream().findFirst().get().getKey();
                    Map<String, Object> responseSchemaMap = (Map<String, Object>) ((Map<String, Object>) responseContentMap.entrySet().stream().findFirst().get().getValue()).get("schema");


                    //解析model
                    Map<String, ModelAttr> definitinMap = parseDefinitions(map);

                    //封装Table
                    Table table = new Table();

                    table.setTitle(title);
                    table.setUrl(url);
                    table.setTag(tag);
                    table.setDescription(description);
                    table.setRequestForm(requestForm);
                    table.setResponseForm(responseForm);
                    table.setRequestType(requestType);
                    //请求体处理
//                    table.setRequestList(processRequestList(requestSchemaMap, definitinMap));
                    //响应体处理
//                    table.setResponseList(processResponseCodeList(responseSchemaMap, definitinMap));

                    result.add(table);
                }

            }
        }

        return map;
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
                        request.setType(request.getType() + ":" + ref.toString().replaceAll("#/definitions/", ""));
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














