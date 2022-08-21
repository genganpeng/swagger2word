package org.word.service;

import org.word.model.ApiTplExcelData;

import java.util.List;
import java.util.Map;

public interface ExportService {
    Map<String, Object> renderTableList(List<ApiTplExcelData> apiTplExcelDataList);
}
