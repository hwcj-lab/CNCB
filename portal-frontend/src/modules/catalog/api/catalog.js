import request from "../../../shared/api/request";
// 资源目录接口集中定义在此处，页面组件不直接拼接后端地址。
/** 获取后端连接状态及目录数量。 */
export const getSystemStatus = () => request.get("/system/status");
/** 获取页面一次性展示的非空推荐分组。 */
export const getResourceRecommendations = (params) => request.get("/resource-recommendations", { params });
/** 将 CNCB 搜索返回的 databases 数组交给后端批量匹配。 */
export const getRecommendationsForSearchResults = (databases) =>
  request.post("/resource-recommendations/search-results", databases);
/** 获取可见数据类型。 */
export const getDataTypes = () => request.get("/data-types");
/** 获取数据库目录，供独立筛选场景使用。 */
export const getDatabases = (params) => request.get("/databases", { params });
/** 获取单个数据库详情。 */
export const getDatabase = (id) => request.get("/databases/" + id);
/** 获取单个数据库的关联工具。 */
export const getDatabaseTools = (id) => request.get("/databases/" + id + "/tools");
