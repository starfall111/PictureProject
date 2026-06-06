// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /space/${param0} */
export async function spaceControllerGetSpaceById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SpaceControllerGetSpaceByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseSpace>(`/space/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /space/add */
export async function spaceControllerAddSpace(
  body: API.SpaceAddDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/space/add", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /space/delete */
export async function spaceControllerDeleteSpace(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/delete", {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /space/edit */
export async function spaceControllerEditSpace(
  body: API.SpaceEditDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/edit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /space/list/level */
export async function spaceControllerListSpaceLevel(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseListSpaceLevel>("/space/list/level", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /space/query */
export async function spaceControllerQuerySpaceListAdmin(
  body: API.SpaceQueryDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSpace>("/space/query", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /space/update */
export async function spaceControllerUpdateSpace(
  body: API.SpaceUpdateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/space/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /space/userSpace/${param0} */
export async function spaceControllerGetSpaceByUserId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.SpaceControllerGetSpaceByUserIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseListSpaceVO>(`/space/userSpace/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}
