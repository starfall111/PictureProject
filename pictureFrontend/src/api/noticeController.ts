// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /verification/send */
export async function noticeControllerSendVerificationCode(
  body: API.SendVerificationCodeDTO,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>("/verification/send", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
