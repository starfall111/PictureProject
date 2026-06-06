// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /admin/ban/list */
export async function adminBanControllerGetBanRecordList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminBanControllerGetBanRecordListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageBanRecordVO>("/admin/ban/list", {
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

/** 此处后端没有提供注释 GET /admin/ban/stats */
export async function adminBanControllerGetBanStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseMapStringObject>("/admin/ban/stats", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/ban/unban */
export async function adminBanControllerUnbanUser(
  body: API.BanUnbanDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/ban/unban", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
