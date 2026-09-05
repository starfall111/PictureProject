package org.example.server.controller;


import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.vo.HealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zou
 */
@RestController
@RequestMapping("/")
public class MainController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping("health")
    public BaseResponse<HealthVO> health() {
        HealthVO healthVO = new HealthVO();
        healthVO.setStatus("ok");
        healthVO.setVersion(buildProperties != null ? buildProperties.getVersion() : "unknown");
        return ResultUtils.success(healthVO);
    }
}
