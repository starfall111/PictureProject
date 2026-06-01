// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** adminRebuild POST /api/recommend/admin/rebuild */
export async function adminRebuildUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseInt_>("/api/recommend/admin/rebuild", {
    method: "POST",
    ...(options || {}),
  });
}

/** recommend POST /api/recommend/query */
export async function recommendUsingPost(
  body: API.RecommendQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRecommendVO_>("/api/recommend/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
