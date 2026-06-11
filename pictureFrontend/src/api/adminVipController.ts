// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /admin/vip/${param0}/grant */
export async function adminVipControllerGrantVip(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminVipControllerGrantVipParams,
  body: API.VipGrantDTO,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseObject>(`/admin/vip/${param0}/grant`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/vip/${param0}/revoke */
export async function adminVipControllerRevokeVip(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminVipControllerRevokeVipParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params;
  return request<API.BaseResponseObject>(`/admin/vip/${param0}/revoke`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/vip/degrade */
export async function adminVipControllerSetDegradeLevel(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminVipControllerSetDegradeLevelParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/vip/degrade", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/vip/list */
export async function adminVipControllerListVipUsers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminVipControllerListVipUsersParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUser>("/admin/vip/list", {
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

/** 此处后端没有提供注释 GET /admin/vip/stats */
export async function adminVipControllerGetVipStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseVipStatsVO>("/admin/vip/stats", {
    method: "GET",
    ...(options || {}),
  });
}
