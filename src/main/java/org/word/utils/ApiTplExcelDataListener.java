package org.word.utils;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.word.model.ApiTplExcelData;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ApiTplExcelDataListener implements ReadListener<ApiTplExcelData> {
    private List<ApiTplExcelData> apiTplExcelDataList;

    public ApiTplExcelDataListener(List<ApiTplExcelData> apiTplExcelDataList) {
        this.apiTplExcelDataList = apiTplExcelDataList;
    }

    @Override
    public void invoke(ApiTplExcelData apiTplExcelData, AnalysisContext analysisContext) {
        try {
            log.info("解析到一条数据:{}", JsonUtils.writeJsonStr(apiTplExcelData));
            apiTplExcelDataList.add(apiTplExcelData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        log.info("所有数据解析完成！size:{}", apiTplExcelDataList.size());
    }
}
