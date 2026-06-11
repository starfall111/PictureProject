package org.example.server.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.common.annotation.CheckAuth;
import org.example.common.context.UserContext;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.dto.seckill.BatchQueryDTO;
import org.example.pojo.dto.seckill.SeckillGrabDTO;
import org.example.pojo.entity.SeckillOrder;
import org.example.pojo.vo.seckill.PublicBatchVO;
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
        String userPhone = UserContext.get().getUserPhone();
        ThrowUtils.throwIf(StrUtil.isBlank(userPhone), ErrorCode.PARAMS_ERROR, "请先绑定手机号再参与本次活动");
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
     * 获取批次列表（公开接口，无需登录）
     * <p>
     * 支持按状态筛选，返回分页结果。
     * 只返回用户可见的批次（预热中/进行中/已结束），不返回草稿和已取消。
     * </p>
     *
     * @param status   可选，状态筛选：1-预热中, 2-进行中, 3-已结束
     * @param current  页码，默认 1
     * @param pageSize 每页条数，默认 10
     * @return 分页批次列表
     */
    @GetMapping("/batch/list")
    public BaseResponse<Page<PublicBatchVO>> listBatches(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        BatchQueryDTO dto = new BatchQueryDTO();
        dto.setStatus(status);
        dto.setCurrent(current);
        dto.setPageSize(pageSize);
        Page<PublicBatchVO> result = seckillService.listBatches(dto);
        return ResultUtils.success(result);
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
