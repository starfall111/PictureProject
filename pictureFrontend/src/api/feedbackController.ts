// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /feedback/${param0} */
export async function feedbackControllerGetFeedbackDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerGetFeedbackDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseFeedbackVO>(`/feedback/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /feedback/${param0}/confirm */
export async function feedbackControllerConfirmFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerConfirmFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/feedback/${param0}/confirm`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /feedback/${param0}/reopen */
export async function feedbackControllerReopenFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerReopenFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/feedback/${param0}/reopen`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /feedback/${param0}/reply */
export async function feedbackControllerReplyFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerReplyFeedbackParams,
  body: API.FeedbackReplyDTO,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/feedback/${param0}/reply`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 DELETE /feedback/${param0}/withdraw */
export async function feedbackControllerWithdrawFeedback(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerWithdrawFeedbackParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseBoolean>(`/feedback/${param0}/withdraw`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /feedback/list */
export async function feedbackControllerGetMyFeedbackList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.FeedbackControllerGetMyFeedbackListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFeedbackListItemVO>("/feedback/list", {
    method: "GET",
    params: {
      ...params,
      dto: undefined,
      ...params["dto"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /feedback/stats */
export async function feedbackControllerGetMyFeedbackStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseFeedbackStatsVO>("/feedback/stats", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /feedback/submit */
export async function feedbackControllerSubmitFeedback(
  body: API.FeedbackSubmitDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/feedback/submit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /feedback/upload */
export async function feedbackControllerUploadAttachment(
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseFeedbackAttachmentVO>("/feedback/upload", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}
