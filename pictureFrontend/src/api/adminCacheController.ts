// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /admin/cache/clearAll */
export async function adminCacheControllerClearAllCache(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseBoolean>("/admin/cache/clearAll", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/cache/picture/clear */
export async function adminCacheControllerClearPictureCache(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseBoolean>("/admin/cache/picture/clear", {
    method: "POST",
    ...(options || {}),
  });
}
