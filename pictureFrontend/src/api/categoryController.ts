// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /category/add */
export async function categoryControllerAddCategory(
  body: API.CategoryAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/category/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /category/delete */
export async function categoryControllerDeleteCategory(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/category/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /category/get/${param0} */
export async function categoryControllerGetCategoryById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.CategoryControllerGetCategoryByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseCategory>(`/category/get/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /category/list */
export async function categoryControllerListCategory(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseListCategory>("/category/list", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /category/page/query */
export async function categoryControllerQueryCategoryPage(
  body: API.CategoryQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageCategory>("/category/page/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /category/update */
export async function categoryControllerUpdateCategory(
  body: API.CategoryUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/category/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
