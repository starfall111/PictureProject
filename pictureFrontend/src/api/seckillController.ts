// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /seckill/batch/${param0} */
export async function seckillControllerGetBatchInfo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillControllerGetBatchInfoParams,
  options?: { [key: string]: any }
) {
  const { batchId: param0, ...queryParams } = params;
  return request<API.BaseResponseMapStringObject>(`/seckill/batch/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /seckill/batch/list */
export async function seckillControllerListBatches(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillControllerListBatchesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePublicBatchVO>("/seckill/batch/list", {
    method: "GET",
    params: {
      // current has a default value: 1
      current: "1",
      // pageSize has a default value: 10
      pageSize: "10",
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /seckill/grab */
export async function seckillControllerGrab(
  body: API.SeckillGrabDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/seckill/grab", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /seckill/result */
export async function seckillControllerGetResult(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillControllerGetResultParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSeckillOrder>("/seckill/result", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /seckill/token */
export async function seckillControllerGetToken(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SeckillControllerGetTokenParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/seckill/token", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
