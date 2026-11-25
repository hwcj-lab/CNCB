import request from "../../../shared/api/request";
/** 获取最近成功快照及全部仪表盘维度。 */
export const getLatestSnapshotResult = (limitPerDimension = 50) =>
  request.get("/research-snapshots/latest/result", {
    params: { limitPerDimension },
  });
/** 查询快照历史版本，用于页面选择和回放。 */
export const getResearchSnapshotVersions = () =>
  request.get("/research-snapshots");
/** 获取指定历史快照的完整仪表盘数据。 */
export const getResearchSnapshotResult = (snapshotId, limitPerDimension = 50) =>
  request.get(`/research-snapshots/${snapshotId}/result`, {
    params: { limitPerDimension },
  });
/** 获取快照内动态存在的数据库统计入口。 */
export const getResearchSnapshotDatabases = (snapshotId) =>
  request.get(`/research-snapshots/${snapshotId}/databases`);
/** 读取某个研究主题下有来源映射的数据库。 */
export const getResearchTopicDatabases = (snapshotId, topicCode) =>
  request.get(`/research-snapshots/${snapshotId}/topics/${encodeURIComponent(topicCode)}/databases`);
/** 分数据库五维统计与总统计使用独立接口，避免来源增加后首屏响应膨胀。 */
export const getDatabaseSnapshotResult = (snapshotId, databaseCode, limitPerDimension = 50) =>
  request.get(`/research-snapshots/${snapshotId}/databases/${encodeURIComponent(databaseCode)}/result`, {
    params: { limitPerDimension },
  });
/** 手工触发一个新的科研统计快照。 */
export const buildResearchSnapshot = (payload) =>
  request.post("/research-snapshots/build", payload);
/** 获取系统自动纳入的全部本地文献来源（只读展示，不用于选择）。 */
export const getResearchSnapshotSources = () =>
  request.get("/research-snapshots/sources");
/** 只修改快照展示名称，不重新执行统计。 */
export const renameResearchSnapshot = (snapshotId, name) =>
  request.patch(`/research-snapshots/${snapshotId}/name`, { name });
/** 请求后台任务在当前批次结束后安全停止。 */
export const stopResearchSnapshot = () =>
  request.post("/research-snapshots/stop");
/** 查询快照后台构建状态。 */
export const getResearchSnapshotStatus = () =>
  request.get("/research-snapshots/status");

/** 查询全局去重文献整合表重建状态。 */
export const getPublicationIntegrationStatus = () =>
  request.get("/research-publications/integration/status");

/** 触发整合表全量重建；数据重建在后台执行。 */
export const rebuildPublicationIntegration = () =>
  request.post("/research-publications/integration/rebuild");
/** 点击图表维度后分页获取构成该统计值的具体文献。 */
export const getDimensionPublications = (snapshotId, dimensionId, page = 1, size = 10) =>
  request.get(
    `/research-snapshots/${snapshotId}/dimensions/${dimensionId}/publications`,
    { params: { page, size } },
  );
/** 查询某数据库范围内一篇文献的双向关联文献。 */
export const getRelatedPublications = (databaseCode, identifierType, identifier, page = 1, size = 20) =>
  request.get(
    "/collection/publication-relations/publications",
    { params: { databaseCode, identifierType, identifier, page, size } },
  );
/** 删除一个统计快照；只级联清理维度和穿透关系，全局文献继续保留。 */
export const deleteResearchSnapshot = (snapshotId) =>
  request.delete(`/research-snapshots/${snapshotId}`);
