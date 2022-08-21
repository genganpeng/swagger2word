package org.word.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.word.model.ApiTplExcelData;
import org.word.model.Table;
import org.word.parser.SwaggerParserContext;
import org.word.service.ExportService;
import org.word.service.WordService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {
    private final WordService tableService;

    @Autowired
    public ExportServiceImpl(WordService tableService) {
        this.tableService = tableService;
    }

    @Override
    public Map<String, Object> renderTableList(List<ApiTplExcelData> apiTplExcelDataList) {
        List<Table> tableReusltList = new ArrayList<>();
        apiTplExcelDataList.stream().parallel().forEach(apiItem -> {
            //获取出解析后的tableList
            List<Table> tableList = tableService.getTableList(apiItem.getApiDocUrl());
            //在里面找到对应的url
            tableList.forEach(tableItem -> {
                if (apiItem.getApiPathUrl().equals(tableItem.getUrl())) {
                    if (StringUtils.isNotBlank(apiItem.getApiMethod())) {
                        //excel中传了请求method时，判断请求类型是否匹配
                        if (apiItem.getApiMethod().equalsIgnoreCase(tableItem.getRequestType())) {
                            //请求类型和url都匹配
                            tableReusltList.add(tableItem);
                        } else {
                            log.info("[renderTableList] method不匹配.url:{} method1:{} method2:{}", tableItem.getUrl(), apiItem.getApiMethod(), tableItem.getRequestType());
                        }
                    } else {
                        tableReusltList.add(tableItem);
                    }
                }
            });
        });
        return null;
    }
}
