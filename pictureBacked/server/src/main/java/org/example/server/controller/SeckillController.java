package org.example.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.SeckillGrabDTO;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.service.SeckillService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀抢购 Controller
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    /**
     * 获取秒杀令牌
     *
     * @param batchId 批次ID
     * @return token + expireIn
     */
    @GetMapping("/token")
    @CheckAuth
    public BaseResponse<Map<String, Object>> getToken(@RequestParam Long batchId, HttpServletRequest request) {
        Long userId = UserContext.get().getId();
        String token = seckillService.getToken(userId, batchId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expireIn", 300);
        return ResultUtils.success(data);
    }

    /**
     * 执行秒杀抢券
     *
     * @param dto 抢券参数（batchId + token）
     * @return orderNo + status
     */
    @PostMapping("/grab")
    @CheckAuth
    public BaseResponse<Map<String, Object>> grab(@RequestBody SeckillGrabDTO dto, HttpServletRequest request) {
        Long userId = UserContext.get().getId();
        String clientIP = getClientIP(request);
        String orderNo = seckillService.grab(userId, dto.getBatchId(), dto.getToken(), clientIP);
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", "PENDING");
        return ResultUtils.success(data);
    }

    /**
     * 查询秒杀结果
     *
     * @param orderNo 订单号
     * @return 秒杀订单
     */
    @GetMapping("/result")
    @CheckAuth
    public BaseResponse<SeckillOrder> getResult(@RequestParam String orderNo) {
        Long userId = UserContext.get().getId();
        SeckillOrder order = seckillService.getResult(userId, orderNo);
        return ResultUtils.success(order);
    }

    /**
     * 获取批次信息（公开接口）
     *
     * @param batchId 批次ID
     * @return 批次详情 + 剩余库存
     */
    @GetMapping("/batch/{batchId}")
    public BaseResponse<Map<String, Object>> getBatchInfo(@PathVariable Long batchId) {
        return ResultUtils.success(seckillService.getBatchInfo(batchId));
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
