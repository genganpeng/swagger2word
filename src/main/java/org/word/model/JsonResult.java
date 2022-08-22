package org.word.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class JsonResult<T> {
    @Schema(name = "msg",description = "描述信息")
    private String msg;
    @Schema(name = "code",description = "代码code")
    private Integer code;
    @Schema(name = "data",description = "返回数据")
    private T data;

    public static <T> JsonResult<T> buildSuccess() {
        JsonResult<T> jsonResult = new JsonResult<T>();
        jsonResult.setCode(200);
        return jsonResult;
    }
}
