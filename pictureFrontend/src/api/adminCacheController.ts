// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** clearAllCache POST /api/admin/cache/clearAll */
export async function clearAllCacheUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>("/api/admin/cache/clearAll", {
    method: "POST",
    ...(options || {}),
  });
}

/** clearPictureCache POST /api/admin/cache/picture/clear */
export async function clearPictureCacheUsingPost(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseBoolean_>("/api/admin/cache/picture/clear", {
    method: "POST",
    ...(options || {}),
  });
}
