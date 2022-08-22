package org.word.model;

import lombok.Data;

@Data
public class ApiTplExcelData {
    private Integer index;
    private String swaggerUrl;
    private String apiPathUrl;
    private String apiMethod;
    /**
     * 大标题
     */
    private String title;
    /**
     * 小标题
     */
    private String tag;
}
