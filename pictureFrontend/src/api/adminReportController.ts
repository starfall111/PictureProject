// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /admin/report/batch-handle */
export async function adminReportControllerBatchHandleReports(
  body: API.ReportHandleDTO[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/report/batch-handle", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/report/detail/${param0} */
export async function adminReportControllerGetAdminReportDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminReportControllerGetAdminReportDetailParams,
  options?: { [key: string]: any }
) {
  const { reportId: param0, ...queryParams } = params;
  return request<API.BaseResponseReportVO>(`/admin/report/detail/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /admin/report/handle */
export async function adminReportControllerHandleReport(
  body: API.ReportHandleDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/admin/report/handle", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/report/list */
export async function adminReportControllerGetAdminReportList(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.AdminReportControllerGetAdminReportListParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReportVO>("/admin/report/list", {
    method: "GET",
    params: {
      ...params,
      dto: undefined,
      ...params["dto"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /admin/report/stats */
export async function adminReportControllerGetAdminReportStats(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseReportStatsVO>("/admin/report/stats", {
    method: "GET",
    ...(options || {}),
  });
}
