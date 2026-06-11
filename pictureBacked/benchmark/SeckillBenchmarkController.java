package org.example.server.controller.benchmark;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.common.constants.RedisKeyConstants;
import org.example.common.exception.ErrorCode;
import org.example.common.exception.ThrowUtils;
import org.example.common.result.BaseResponse;
import org.example.common.result.ResultUtils;
import org.example.pojo.entity.SeckillOrder;
import org.example.server.service.SeckillService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 秒杀压测专用 Controller
 *
 * <p>仅在 {@code spring.profiles.active=benchmark} 时加载，生产环境不会注册此 Bean。
 * 跳过 session 鉴权，通过请求参数直接传入 userId，完整复用 Redis + MQ 秒杀链路。</p>
 *
 * @author Zou
 */
@Slf4j
@RestController
@RequestMapping("/benchmark/seckill")
@Profile("benchmark")
public class SeckillBenchmarkController {

    @Resource
    private SeckillService seckillService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取秒杀令牌（无需 session）
     *
     * @param userId  用户ID
     * @param batchId 批次ID
     * @return token + expireIn
     */
    @GetMapping("/token")
    public BaseResponse<Map<String, Object>> getToken(
            @RequestParam Long userId,
            @RequestParam Long batchId) {
        ThrowUtils.throwIf(userId == null || batchId == null, ErrorCode.PARAMS_ERROR);
        String token = seckillService.getToken(userId, batchId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expireIn", 300);
        return ResultUtils.success(data);
    }

    /**
     * 执行秒杀抢券（无需 session）
     *
     * @param request 请求体：userId + batchId + token
     * @return orderNo + status
     */
    @PostMapping("/grab")
    public BaseResponse<Map<String, Object>> grab(@RequestBody BenchmarkGrabRequest request) {
        ThrowUtils.throwIf(request == null
                        || request.getUserId() == null
                        || request.getBatchId() == null
                        || StrUtil.isBlank(request.getToken()),
                ErrorCode.PARAMS_ERROR, "参数不完整");

        // 支持模拟多IP：请求体可传 clientIP，不传则自动随机生成
        String clientIP = StrUtil.isNotBlank(request.getClientIP())
                ? request.getClientIP()
                : randomIP();
        String orderNo = seckillService.grab(
                request.getUserId(),
                request.getBatchId(),
                request.getToken(),
                clientIP
        );
        Map<String, Object> data = new HashMap<>();
        data.put("orderNo", orderNo);
        data.put("status", "PENDING");
        return ResultUtils.success(data);
    }

    /**
     * 查询秒杀结果（无需 session）
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 秒杀订单
     */
    @GetMapping("/result")
    public BaseResponse<SeckillOrder> getResult(
            @RequestParam Long userId,
            @RequestParam String orderNo) {
        SeckillOrder order = seckillService.getResult(userId, orderNo);
        return ResultUtils.success(order);
    }

    /**
     * 一键预热库存到 Redis
     *
     * @param batchId 批次ID
     * @return 操作结果
     */
    @PostMapping("/init")
    public BaseResponse<String> init(@RequestParam Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);
        seckillService.preheat(batchId);
        return ResultUtils.success("预热完成");
    }

    /**
     * 压测实时统计
     *
     * @param batchId 批次ID
     * @return Redis 剩余库存、版本号、批次缓存信息
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> stats(@RequestParam Long batchId) {
        ThrowUtils.throwIf(batchId == null, ErrorCode.PARAMS_ERROR);
        Map<String, Object> data = new HashMap<>();

        // 读取 Redis 库存
        String stockKey = String.format(RedisKeyConstants.SECKILL_BATCH_STOCK, batchId);
        Map<Object, Object> stockFields = stringRedisTemplate.opsForHash().entries(stockKey);
        data.put("stock", stockFields.get("stock"));
        data.put("version", stockFields.get("version"));

        // 读取批次缓存
        String batchInfoKey = String.format(RedisKeyConstants.SECKILL_BATCH_INFO, batchId);
        String batchInfoJson = stringRedisTemplate.opsForValue().get(batchInfoKey);
        if (StrUtil.isNotBlank(batchInfoJson)) {
            data.put("batchInfo", JSONUtil.parseObj(batchInfoJson));
        } else {
            data.put("batchInfo", "未缓存");
        }

        return ResultUtils.success(data);
    }

    /**
     * 压测抢券请求体
     */
    /**
     * 生成随机 IP（10.x.x.x 内网段，避免与真实 IP 冲突）
     */
    private static String randomIP() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(1, 255) + "." + r.nextInt(0, 256) + "." + r.nextInt(1, 255);
    }

    /**
     * 压测抢券请求体
     */
    @lombok.Data
    public static class BenchmarkGrabRequest {
        private Long userId;
        private Long batchId;
        private String token;
        /** 可选：模拟的客户端 IP，不传则自动随机生成 */
        private String clientIP;
    }
}
