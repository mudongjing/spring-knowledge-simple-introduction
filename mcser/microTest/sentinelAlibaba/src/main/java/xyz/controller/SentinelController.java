package xyz.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sentinel")
public class SentinelController {

    @RequestMapping("/add")
    public String add(){

        return "add";
    }

    //BlockExceptionHandler接口，可用于统一处理BlockException
}
