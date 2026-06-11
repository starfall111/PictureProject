// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /admin/batch/${param0} */
export async function adminBatchControllerGetBatchDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerGetBatchDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseCodeCouponBatch>(`/admin/batch/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /admin/batch/${param0} */
export async function adminBatchControllerUpdateBatch(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerUpdateBatchParams,
  body: API.BatchUpdateDTO,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/batch/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/${param0}/cancel */
export async function adminBatchControllerCancel(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerCancelParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/batch/${param0}/cancel`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/${param0}/end */
export async function adminBatchControllerEnd(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerEndParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/batch/${param0}/end`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/${param0}/generate-codes */
export async function adminBatchControllerGenerateCodes(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerGenerateCodesParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/admin/batch/${param0}/generate-codes`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /admin/batch/${param0}/restore */
export async function adminBatchControllerRestore(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerRestoreParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/batch/${param0}/restore`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/${param0}/transition */
export async function adminBatchControllerTransition(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerTransitionParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/batch/${param0}/transition`, {
    method: "POST",
    params: {
      ...queryParams,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/create */
export async function adminBatchControllerCreateBatch(
  body: API.BatchCreateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/admin/batch/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/batch/list */
export async function adminBatchControllerListBatches(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchControllerListBatchesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCodeCouponBatch>("/admin/batch/list", {
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

/** 此处后端没有提供注释 GET /admin/batch/stats */
export async function adminBatchControllerGetSeckillStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseSeckillStatsVO>("/admin/batch/stats", {
    method: "GET",
    ...(options || {}),
  });
}
