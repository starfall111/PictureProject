// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** addCategory POST /api/category/add */
export async function addCategoryUsingPost(
  body: API.CategoryAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>("/api/category/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** deleteCategory DELETE /api/category/delete */
export async function deleteCategoryUsingDelete(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/category/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** getCategoryById GET /api/category/get/${param0} */
export async function getCategoryByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCategoryByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseCategory_>(`/api/category/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** listCategory GET /api/category/list */
export async function listCategoryUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListCategory_>("/api/category/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** queryCategoryPage POST /api/category/page/query */
export async function queryCategoryPageUsingPost(
  body: API.CategoryQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCategory_>("/api/category/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** updateCategory POST /api/category/update */
export async function updateCategoryUsingPost(
  body: API.CategoryUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>("/api/category/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
