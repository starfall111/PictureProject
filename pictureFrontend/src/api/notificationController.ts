// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 DELETE /notification/${param0} */
export async function notificationControllerDeleteNotification(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.NotificationControllerDeleteNotificationParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/notification/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /notification/clean/read */
export async function notificationControllerCleanReadNotifications(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseInteger>("/notification/clean/read", {
    method: "DELETE",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /notification/list */
export async function notificationControllerListNotifications(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.NotificationControllerListNotificationsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageNotificationVO>("/notification/list", {
    method: "GET",
    params: {
      ...params,
      queryDTO: undefined,
      ...params["queryDTO"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /notification/read/${param0} */
export async function notificationControllerMarkAsRead(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.NotificationControllerMarkAsReadParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/notification/read/${param0}`, {
    method: "PUT",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PUT /notification/read/all */
export async function notificationControllerMarkAllAsRead(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseBoolean>("/notification/read/all", {
    method: "PUT",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /notification/sse */
export async function notificationControllerConnectSse(options?: {
  [key: string]: any;
}) {
  return request<API.SseEmitter>("/notification/sse", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /notification/unread/count */
export async function notificationControllerGetUnreadCount(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseLong>("/notification/unread/count", {
    method: "GET",
    ...(options || {}),
  });
}
