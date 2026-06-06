// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /admin/feedback/${param0} */
export async function adminFeedbackControllerGetAdminFeedbackDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerGetAdminFeedbackDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseFeedbackVO>(`/admin/feedback/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/claim */
export async function adminFeedbackControllerClaimFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerClaimFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/feedback/${param0}/claim`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/convert */
export async function adminFeedbackControllerConvertToReport(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerConvertToReportParams,
  body: API.FeedbackConvertDTO,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseLong>(`/admin/feedback/${param0}/convert`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/note */
export async function adminFeedbackControllerAddInternalNote(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerAddInternalNoteParams,
  body: API.FeedbackReplyDTO,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/feedback/${param0}/note`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/priority */
export async function adminFeedbackControllerUpdatePriority(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerUpdatePriorityParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/admin/feedback/${param0}/priority`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/reject */
export async function adminFeedbackControllerRejectFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerRejectFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/feedback/${param0}/reject`, {
    method: "POST",
    params: {
      ...queryParams,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/reply */
export async function adminFeedbackControllerAdminReplyFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerAdminReplyFeedbackParams,
  body: API.FeedbackReplyDTO,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/admin/feedback/${param0}/reply`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/feedback/${param0}/transfer */
export async function adminFeedbackControllerTransferFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerTransferFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(
    `/admin/feedback/${param0}/transfer`,
    {
      method: "POST",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /admin/feedback/batch-close */
export async function adminFeedbackControllerBatchCloseFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerBatchCloseFeedbackParams,
  body: number[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/feedback/batch-close", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/feedback/list */
export async function adminFeedbackControllerGetAdminFeedbackList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminFeedbackControllerGetAdminFeedbackListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFeedbackListItemVO>(
    "/admin/feedback/list",
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

/** 此处后端没有提供注释 GET /admin/feedback/stats */
export async function adminFeedbackControllerGetAdminFeedbackStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseFeedbackStatsVO>("/admin/feedback/stats", {
    method: "GET",
    ...(options || {}),
  });
}
