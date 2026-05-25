// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** addTag POST /api/tag/add */
export async function addTagUsingPost(
  body: API.TagAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/tag/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteTag DELETE /api/tag/delete */
export async function deleteTagUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/tag/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getTagById GET /api/tag/get/${param0} */
export async function getTagByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTagByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseTag_>(`/api/tag/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** listTag GET /api/tag/list */
export async function listTagUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListTag_>("/api/tag/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** queryTagPage POST /api/tag/page/query */
export async function queryTagPageUsingPost(
  body: API.TagQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageTag_>("/api/tag/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateTag POST /api/tag/update */
export async function updateTagUsingPost(
  body: API.TagUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/tag/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
