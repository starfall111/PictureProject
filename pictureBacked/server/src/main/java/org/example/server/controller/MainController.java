package org.example.pictureBacked.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import result.BaseResponse;
import result.ResultUtils;

/**
 * @author Zou
 */
@RestController
@RequestMapping("/")
public class MainController {

    @GetMapping("health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
