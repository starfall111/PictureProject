// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /admin/batch/task/${param0} */
export async function adminBatchTaskControllerGetAdminDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchTaskControllerGetAdminDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseAdminBatchTaskVO>(
    `/admin/batch/task/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 DELETE /admin/batch/task/delete */
export async function adminBatchTaskControllerAdminDelete(
  body: number,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/batch/task/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/batch/task/list */
export async function adminBatchTaskControllerGetAdminList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBatchTaskControllerGetAdminListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAdminBatchTaskVO>(
    "/admin/batch/task/list",
    {
      method: "GET",
      params: {
        ...params,
        dto: undefined,
        ...params["dto"],
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /admin/batch/task/stats */
export async function adminBatchTaskControllerGetAdminStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseBatchTaskStatsVO>("/admin/batch/task/stats", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/batch/task/update */
export async function adminBatchTaskControllerAdminUpdate(
  body: API.BatchTaskUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/batch/task/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
