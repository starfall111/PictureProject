// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** deleteNotification DELETE /api/notification/${param0} */
export async function deleteNotificationUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteNotificationUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/notification/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** cleanReadNotifications DELETE /api/notification/clean/read */
export async function cleanReadNotificationsUsingDelete(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseInt_>("/api/notification/clean/read", {
    method: "DELETE",
    ...(options || {}),
  });
}

/** listNotifications GET /api/notification/list */
export async function listNotificationsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listNotificationsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageNotificationVO_>(
    "/api/notification/list",
    {
      method: "GET",
      params: {
        ...params,
      },
      ...(options || {}),
    }
  );
}

/** markAsRead PUT /api/notification/read/${param0} */
export async function markAsReadUsingPut(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.markAsReadUsingPUTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean_>(`/api/notification/read/${param0}`, {
    method: "PUT",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** markAllAsRead PUT /api/notification/read/all */
export async function markAllAsReadUsingPut(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>("/api/notification/read/all", {
    method: "PUT",
    ...(options || {}),
  });
}

/** connectSse GET /api/notification/sse */
export async function connectSseUsingGet(options?: { [key: string]: any }) {
  return request<API.SseEmitter>("/api/notification/sse", {
    method: "GET",
    ...(options || {}),
  });
}

/** getUnreadCount GET /api/notification/unread/count */
export async function getUnreadCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>("/api/notification/unread/count", {
    method: "GET",
    ...(options || {}),
  });
}
