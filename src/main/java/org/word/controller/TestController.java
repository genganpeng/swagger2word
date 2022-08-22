package org.word.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.word.model.JsonResult;

import java.util.List;

@RequestMapping(value = "test")
@RestController
@Tag(name = "测试api", description = "测试用的")
public class TestController {


    @PostMapping(value = "/hello")
    @Operation(summary = "测试一个post接口")
    public JsonResult<TestResultParam> hello(@Parameter(description = "测试的参数对象") @RequestBody TestInputParam testInputParam) {
        return JsonResult.buildSuccess();
    }

    @Data
    private static class TestInputParam {
        @Schema(name = "id", description = "主键id")
        private Long id;
        @Schema(name = "code", description = "编码")
        private String code;
        @Schema(name = "testInputParam2", description = "另一个对象")
        private TestInputParam2 testInputParam2;
    }

    @Data
    private static class TestInputParam2 {
        @Schema(name = "filters", description = "过滤列表")
        private List<String> filters;
    }

    @Data
    private static class TestResultParam {
        @Schema(name = "dataList", description = "返回结果列表")
        private List<String> dataList;
    }
}
