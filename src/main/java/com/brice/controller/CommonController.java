package com.brice.controller;

import com.brice.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        //截取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUID生成新文件名
        String fileName = UUID.randomUUID() + suffix;

        File dir = new File(basePath);
        //如果目录不存在，先创建
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return R.success(fileName);
    }

    /**
     * 文件下载
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) throws IOException {
        //获取文件输入流
        FileInputStream inputStream = new FileInputStream(basePath + name);

        ServletOutputStream outputStream = response.getOutputStream();

        response.setContentType("image/jpeg");

        //输入流，输出流配合将数据写回浏览器
        int len;
        byte[] bytes = new byte[1024];
        while ((len = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, len);
            outputStream.flush();
        }

        outputStream.close();
        inputStream.close();
    }
}
