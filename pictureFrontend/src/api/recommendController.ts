// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /recommend/admin/rebuild */
export async function recommendControllerAdminRebuild(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseInteger>("/recommend/admin/rebuild", {
    method: "POST",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /recommend/query */
export async function recommendControllerRecommend(
  body: API.RecommendQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRecommendVO>("/recommend/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
