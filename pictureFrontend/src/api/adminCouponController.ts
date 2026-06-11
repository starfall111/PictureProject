// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /admin/coupon/${param0}/revoke */
export async function adminCouponControllerRevoke(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminCouponControllerRevokeParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseObject>(`/admin/coupon/${param0}/revoke`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/coupon/list */
export async function adminCouponControllerListCoupons(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminCouponControllerListCouponsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCodeCoupon>("/admin/coupon/list", {
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
