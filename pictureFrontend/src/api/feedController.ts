// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /feed/mark-read */
export async function feedControllerMarkRead(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>("/feed/mark-read", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /feed/timeline */
export async function feedControllerGetTimeline(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedControllerGetTimelineParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseFeedTimelineVO>("/feed/timeline", {
    method: "GET",
    params: {
      ...params,
      queryDTO: undefined,
      ...params["queryDTO"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /feed/unread-count */
export async function feedControllerGetUnreadCount(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseFeedUnreadVO>("/feed/unread-count", {
    method: "GET",
    ...(options || {}),
  });
}
