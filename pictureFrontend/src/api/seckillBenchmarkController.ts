// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /benchmark/seckill/grab */
export async function seckillBenchmarkControllerGrab(
  body: API.BenchmarkGrabRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/benchmark/seckill/grab", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /benchmark/seckill/init */
export async function seckillBenchmarkControllerInit(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillBenchmarkControllerInitParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/benchmark/seckill/init", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /benchmark/seckill/result */
export async function seckillBenchmarkControllerGetResult(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillBenchmarkControllerGetResultParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSeckillOrder>("/benchmark/seckill/result", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /benchmark/seckill/stats */
export async function seckillBenchmarkControllerStats(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillBenchmarkControllerStatsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/benchmark/seckill/stats", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /benchmark/seckill/token */
export async function seckillBenchmarkControllerGetToken(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillBenchmarkControllerGetTokenParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/benchmark/seckill/token", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
