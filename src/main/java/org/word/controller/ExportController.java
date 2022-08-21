package org.word.controller;

import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.word.model.ApiTplExcelData;
import org.word.utils.ApiTplExcelDataListener;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RequestMapping(value = "/export/excel")
@Controller
@Tag(name = "ExportController", description = "支持excel模板方式过滤导出")
public class ExportController {

    @Operation(summary = "下载excel模板", description = "excel模板下载")
    @GetMapping(value = "/template/file/download")
    public void getExportExcelTemplateFile(HttpServletResponse response) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("static/apiTpl.xls");
        InputStream inputStream = classPathResource.getInputStream();
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        try (BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream())) {
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode("apiTpl.xls", "utf-8"));
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            bos.write(bytes, 0, bytes.length);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inputStream.close();
        }
    }

    @Operation(summary = "预览", description = "预览")
    @PostMapping(value = "preview", consumes = "multipart/form-data")
    public String preView(Model model, @Parameter(description = "excelFile") @Valid @RequestPart("excelFile") MultipartFile excelFile) throws Exception {
        List<ApiTplExcelData> apiTplExcelDataList = new ArrayList<>();
        //读取完毕
        EasyExcel.read(excelFile.getInputStream(), ApiTplExcelData.class, new ApiTplExcelDataListener(apiTplExcelDataList)).sheet().doRead();
        //
        apiTplExcelDataList.size();
        return "";
    }

}
