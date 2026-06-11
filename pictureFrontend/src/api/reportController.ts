// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /report/cancel */
export async function reportControllerCancelReport(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.ReportControllerCancelReportParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/report/cancel", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /report/detail/${param0} */
export async function reportControllerGetReportDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.ReportControllerGetReportDetailParams,
  options?: { [key: string]: any }
) {
  const { reportId: param0, ...queryParams } = params;
  return request<API.BaseResponseReportVO>(`/report/detail/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /report/my-list */
export async function reportControllerGetMyReportList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.ReportControllerGetMyReportListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReportVO>("/report/my-list", {
    method: "GET",
    params: {
      ...params,
      dto: undefined,
      ...params["dto"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /report/submit */
export async function reportControllerSubmitReport(
  body: API.ReportSubmitDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>("/report/submit", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
