package org.example.bootstrap.web;


import org.example.shared.result.BaseResponse;
import org.example.shared.result.ResultUtils;
import org.example.bootstrap.web.HealthVO;
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
        healthVO.setBuildTime(buildProperties != null ? buildProperties.getTime().toString() : null);
        return ResultUtils.success(healthVO);
    }
}
