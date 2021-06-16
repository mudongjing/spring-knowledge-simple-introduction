package org.example.controller.upload;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Controller
public class FileUploadController {
    @PostMapping("/fileupload")
    public String fileupload(@RequestParam("upfile") MultipartFile file, Model model){
        if (!file.isEmpty()) {
            try {
                // 文件存放服务端的位置
                String rootPath = "I:/tmp";
                File dir = new File(rootPath + File.separator + "tmpFiles");
                if (!dir.exists())
                    dir.mkdirs();
                // 写文件到服务器
                File serverFile = new File(dir.getAbsolutePath() +
                        File.separator + file.getOriginalFilename());
                file.transferTo(serverFile);
                model.addAttribute("message",
                        "你的文件"+file.getOriginalFilename()+"已上传成功！");
            } catch (Exception e) {
                model.addAttribute("message",
                        "You failed to upload " +
                                file.getOriginalFilename() + " => " +
                                e.getMessage());
            }
        } else {
            model.addAttribute("message",
                    "You failed to upload " +
                            file.getOriginalFilename() +
                            " because the file was empty.") ;
        }
        return "upload/upload";
    }
    @GetMapping("/fileupload")
    public String file(Model model){
        model.addAttribute("message","TEST");
        return "upload/upload";
    }
}
