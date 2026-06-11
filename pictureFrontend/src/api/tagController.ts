// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /tag/add */
export async function tagControllerAddTag(
  body: API.TagAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/tag/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /tag/delete */
export async function tagControllerDeleteTag(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/tag/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /tag/get/${param0} */
export async function tagControllerGetTagById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.TagControllerGetTagByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseTag>(`/tag/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /tag/list */
export async function tagControllerListTag(options?: { [key: string]: any }) {
  return request<API.BaseResponseListTag>("/tag/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /tag/page/query */
export async function tagControllerQueryTagPage(
  body: API.TagQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageTag>("/tag/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /tag/update */
export async function tagControllerUpdateTag(
  body: API.TagUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/tag/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
