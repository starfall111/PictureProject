// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 PUT /admin/system-message */
export async function adminSystemMessageControllerUpdateSystemMessage(
  body: API.SystemMessageCreateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/system-message", {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/system-message */
export async function adminSystemMessageControllerCreateSystemMessage(
  body: API.SystemMessageCreateDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/admin/system-message", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /admin/system-message/${param0} */
export async function adminSystemMessageControllerDeleteSystemMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminSystemMessageControllerDeleteSystemMessageParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/system-message/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/system-message/list */
export async function adminSystemMessageControllerListSystemMessages(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminSystemMessageControllerListSystemMessagesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemMessageVO>(
    "/admin/system-message/list",
    {
      method: "GET",
      params: {
        ...params,
        dto: undefined,
        ...params["dto"],
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /admin/system-message/publish/${param0} */
export async function adminSystemMessageControllerPublishSystemMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminSystemMessageControllerPublishSystemMessageParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/admin/system-message/publish/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /admin/system-message/revoke/${param0} */
export async function adminSystemMessageControllerRevokeSystemMessage(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminSystemMessageControllerRevokeSystemMessageParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/admin/system-message/revoke/${param0}`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}
