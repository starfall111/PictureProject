// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** markRead POST /api/feed/mark-read */
export async function markReadUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>("/api/feed/mark-read", {
    method: "POST",
    ...(options || {}),
  });
}

/** getTimeline GET /api/feed/timeline */
export async function getTimelineUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTimelineUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseFeedTimelineVO_>("/api/feed/timeline", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** getUnreadCount GET /api/feed/unread-count */
export async function getUnreadCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseFeedUnreadVO_>("/api/feed/unread-count", {
    method: "GET",
    ...(options || {}),
  });
}
