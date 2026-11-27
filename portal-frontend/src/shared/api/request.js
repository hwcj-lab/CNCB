import axios from "axios";
// Axios 公共实例：开发环境默认经 Vite 将 /api 代理到后端。
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 15000,
});
request.interceptors.response.use(
  // 组件只接收业务 data；非 OK 响应统一转为可展示的 Error。
  (response) => {
    const body = response.data;
    if (body?.code !== "OK")
      return Promise.reject(new Error(body?.message || "接口返回格式无效"));
    return body.data;
  },
  (error) =>
    Promise.reject(
      new Error(
        error.response?.data?.message ||
          (error.code === "ECONNABORTED"
            ? "请求超时，请稍后重试"
            : ""),
      ),
    ),
);
export default request;
