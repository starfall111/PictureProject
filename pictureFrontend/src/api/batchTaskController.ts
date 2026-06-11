// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /batch/task/${param0} */
export async function batchTaskControllerGetTask(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.BatchTaskControllerGetTaskParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params;
  return request<API.BaseResponseBatchTaskVO>(`/batch/task/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /batch/task/list */
export async function batchTaskControllerListTasks(options?: {
  [key: string]: any;
}) {
  return request<API.BaseResponseListBatchTaskVO>("/batch/task/list", {
    method: "GET",
    ...(options || {}),
  });
}
