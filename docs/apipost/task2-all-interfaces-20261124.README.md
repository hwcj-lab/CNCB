# 任务二全部有效接口

导入同目录task2-all-interfaces-20261124.json（Postman Collection v2.1，ApiPost可导入），共50个当前Controller接口。

## 目录

- 01 采集部分
  - 01 公共：关联文献采集与查询（7）
  - 02 OpenLB专用：来源盘点及本地分流（6）
  - 03 OpenLB专用：主文献补缺（5）
  - 04 GenBase专用：主文献采集与补全（4）
  - 05 GSA专用：主文献采集与补全（5）
  - 06 BioProject专用：项目文献采集与补全（5）
- 02 统计部分
  - 01 公共：全局去重整合表（2）
  - 02 公共：快照生成与维护（9）
  - 03 总统计：仪表盘及维度（3）
  - 04 分数据库：总览及独立统计（3）
  - 05 公共：图表到具体论文穿透（1）

全部使用http://127.0.0.1:8080完整地址，无环境变量。各请求description含用途、参数范围、操作影响和前置条件。snapshotId、dimensionId、jobId的示例1须换为实际返回ID；示例PMID也须换为目标库已有值。

调用顺序：主采集/本地分流→公共关联任务→必要时重建整合表→生成快照→轮询快照状态→总统计/分库统计→文献穿透。

状态、停止、恢复只在所属流程内共用，不存在覆盖全部采集器和快照任务的统一停止接口。JSON包含真实启动、重置和删除操作，请按需要单独调用。

新增资源解析器使用公共关联接口；ASCancerAtlas官方knowledge接口当前返回404，节点失败并停止，不视为关联终点。详情参见research/task2_research_analysis/resource-resolvers-20261124.md。

通过scripts/build-task2-collection.py可重新生成该集合，并自动核对Controller路径覆盖。生成器不调用任何采集接口。

本次新增功能单独导入task2-gsa-bioproject-20261124.json（17个请求，含共用关联接口），不会修改bio-portal-api.json。完整流程见后端src/main/java/com/example/bioplatform/task2/docs/TASK2_GSA_BIOPROJECT_WORKFLOW.md。
