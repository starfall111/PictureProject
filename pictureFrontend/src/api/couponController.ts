// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /coupon/activate */
export async function couponControllerActivateCoupon(
  body: API.CouponActivateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCouponActivateVO>("/coupon/activate", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /coupon/my */
export async function couponControllerListMyCoupons(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.CouponControllerListMyCouponsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCouponVO>("/coupon/my", {
    method: "GET",
    params: {
      // page has a default value: 1
      page: "1",
      // size has a default value: 10
      size: "10",
      ...params,
    },
    ...(options || {}),
  });
}
