package org.word.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.word.model.Table;
import org.word.parser.SwaggerParserContext;
import org.word.service.WordService;
import org.word.utils.JsonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author XiuYin.Cui
 * @Date 2018/1/12
 **/
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
@Service
public class WordServiceImpl implements WordService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Map<String, Object> tableList(String swaggerUrl) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String jsonStr = restTemplate.getForObject(swaggerUrl, String.class);
            resultMap = tableListFromString(jsonStr);
            log.debug(JsonUtils.writeJsonStr(resultMap));
        } catch (Exception e) {
            log.error("parse error", e);
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> tableListFromString(String jsonStr) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            //表格列表，用来存放解析结果，填充到表格中
            List<Table> tableList = new ArrayList<>();
            //解析策略上下文,用来处理不同版本的解析
            SwaggerParserContext swaggerParserContext = new SwaggerParserContext(jsonStr);
            //进行解析
            Map<String, Object> apiMap = swaggerParserContext.doParse(tableList);
            Map<String, List<Table>> tableMap = tableList.stream().parallel().collect(Collectors.groupingBy(Table::getTitle));
            resultMap.put("tableMap", new TreeMap<>(tableMap));
            resultMap.put("info", apiMap.get("info"));

            log.debug(JsonUtils.writeJsonStr(resultMap));
        } catch (Exception e) {
            log.error("parse error", e);
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> tableList(MultipartFile jsonFile) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String jsonStr = new String(jsonFile.getBytes());
            resultMap = tableListFromString(jsonStr);
            log.debug(JsonUtils.writeJsonStr(resultMap));
        } catch (Exception e) {
            log.error("parse error", e);
        }
        return resultMap;
    }

    @Override
    public List<Table> getTableList(String swaggerUrl) {
        List<Table> tableList = new ArrayList<>();
        Map<String, Object> resultMap = new HashMap<>();
        try {
            String jsonStr = restTemplate.getForObject(swaggerUrl, String.class);
            //解析策略上下文,用来处理不同版本的解析
            SwaggerParserContext swaggerParserContext = new SwaggerParserContext(jsonStr);
            //进行解析
            swaggerParserContext.doParse(tableList);
            log.debug(JsonUtils.writeJsonStr(resultMap));
            return tableList;
        } catch (Exception e) {
            log.error("parse error", e);
        }
        return tableList;
    }
}
