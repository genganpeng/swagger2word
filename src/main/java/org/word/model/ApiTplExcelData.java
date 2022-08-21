package org.word.model;

import lombok.Data;

@Data
public class ApiTplExcelData {
    private Integer index;
    private String swaggerUrl;
    private String apiPathUrl;
    private String apiMethod;
    private String title;
}
