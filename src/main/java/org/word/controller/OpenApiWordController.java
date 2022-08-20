package org.word.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.word.service.OpenApiWordService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

@Controller
@Tag(name = "OpenAPI",description = "此处接口未做3.0适配")
public class OpenApiWordController {

    @Value("${swagger.url}")
    private String swaggerUrl;

    @Autowired
    private OpenApiWordService openApiWordService;
    @Resource
    private SpringTemplateEngine springTemplateEngine;

    private String fileName;

    /**
     * 将 swagger json文件转换成 word文档并下载
     *
     * @param model
     * @param jsonFile 需要转换成 word 文档的swagger json文件
     * @param response
     * @return
     * @throws Exception
     */
    @Operation(summary = "将 swagger json文件转换成 word文档并下载", description = "")
    @RequestMapping(value = "/OpenApiFileToWord", method = {RequestMethod.POST}, consumes = "multipart/form-data")
    public void getWord(Model model, @Parameter(description = "swagger json file") @Valid @RequestPart("jsonFile") MultipartFile jsonFile, HttpServletResponse response) throws Exception {
        generateModelData(model, jsonFile);
        writeContentToResponse(model, response);
    }

    private void generateModelData(Model model, MultipartFile jsonFile) throws IOException {
        Map<String, Object> result = openApiWordService.tableList(jsonFile);
        fileName = jsonFile.getOriginalFilename();

        if (fileName != null) {
            fileName = fileName.replaceAll(".json", "");
        } else {
            fileName = "toWord";
        }

        model.addAttribute("url", "http://");
        model.addAttribute("download", 0);
        model.addAllAttributes(result);
    }

    private void generateModelData(Model model, String url, Integer download) {
        url = StringUtils.defaultIfBlank(url, swaggerUrl);
        Map<String, Object> result = openApiWordService.tableList(url);
        model.addAttribute("url", url);
        model.addAttribute("download", download);
        model.addAllAttributes(result);
    }

    private void writeContentToResponse(Model model, HttpServletResponse response) {
        Context context = new Context();
        context.setVariables(model.asMap());
        String content = springTemplateEngine.process("word", context);
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        try (BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName + ".doc", "utf-8"));
            byte[] bytes = content.getBytes();
            bos.write(bytes, 0, bytes.length);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}