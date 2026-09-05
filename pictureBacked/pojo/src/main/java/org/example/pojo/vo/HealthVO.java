package org.example.pojo.vo;

import lombok.Data;

/**
 * 健康检查视图对象
 *
 * @author Zou
 */
@Data
public class HealthVO {
    private String status;
    private String version;
    private String buildTime;
}
