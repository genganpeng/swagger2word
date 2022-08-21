package org.word.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ApiTplExcelData {
    private Integer index;
    private String apiDocUrl;
    private String apiPathUrl;
    private String apiMethod;
    private String title;
}
