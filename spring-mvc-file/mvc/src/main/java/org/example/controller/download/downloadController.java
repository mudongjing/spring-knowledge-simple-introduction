package org.example.controller.download;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
public class downloadController {
    @GetMapping(value = "/downloadfile")
    public void download(HttpServletRequest request,
                           HttpServletResponse response) throws UnsupportedEncodingException {
        String datadir = request.getServletContext().getRealPath("/resources");
        Path file = Paths.get(datadir, "data.txt");
        if (Files.exists(file)) {
            response.setContentType("text/plain");
            response.addHeader("Content-Disposition",
                    "attachment;filename="+ "data.txt");
            try {
                Files.copy(file, response.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
