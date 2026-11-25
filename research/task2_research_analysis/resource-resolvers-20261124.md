# cncbBanks资源关联解析（2026-11-24）

全部解析器实现RelatedResourceResolver，由Spring自动发现；关联文献通过原公共作业接口执行。OpenLB主采集仍只获取主文献并分流，不自动启动关联作业。

| cncbBanks.db | 解析路径 | 当前验证 |
| --- | --- | --- |
| BioCode | BT去前缀后调用biocode/api/tool/{id}，读publications及grants | 保留已有实现 |
| Database Commons | DBC编号调用databasecommons/api/biodb/cited?dbId=，读目标资源引用及funding | 保留已有实现 |
| BioProject | gwh/api/public/bioProject/{PRJCA}，读listPublication[].pubmedId和listGrants | PRJCA000091接口SUCCESS，返回5个PMID |
| GSA | gsa/search?searchTerm={CRA}读取关联项目，再调用BioProject解析器 | CRA015930页面关联PRJCA025130；详情browse页曾返回SecurityCheck，代码使用搜索入口 |
| GWH | 数字Assembly详情页读取明确PubMed/OpenLB论文链接及关联BioProject；只有组装号时调用公开assembly接口（去版本号） | Assembly/104189/show直接列出PMID 21515709及PRJCA049391 |
| ASCancerAtlas | ascancer/api/browse/knowledge?eventid=...&page=0&size=100，按items分页读pubmedid | 接口地址来自当前官方详情页面脚本；实际返回404，暂未通过线上数据验证，不声称可用 |

## 运行与错误处理

POST /api/collection/publication-relations/jobs/start-pending-batch，databaseCode填source_code，例如GSA、GWH、BIOPROJECT、ASCANCERATLAS。

- 继续使用现有公共队列和关联关系表，无新增表或迁移SQL。
- HTTP异常、安全检查页、结构不符不视为无关联，节点FAILED并停止作业；job状态和lastError反馈原因。
- 修复接口后，POST /jobs/{jobId}/resume会重置该作业的FAILED节点并继续；已成功节点不重跑。
- 新解析器共享最多256条、15分钟的成功响应缓存；请求间隔至少800ms、连接10秒、读取25秒超时。
- ASCancerAtlas eventid中的裸+按正链符号编码为%2B，不能按表单解码为空格。
- ASCancerAtlas端点可在application.yml的app.ascancer.knowledge-endpoint中修改；当前官方接口恢复前不建议启动该库批量关联任务。
- 只提取明确的PMID字段/论文链接，不将资源数字、基金编号、位置数字当作PMID；目前这条关联流程仍以PMID下钻，不新增DOI-only文献查询。
- GSA/GWH经项目扩展得到的是项目明确列出的相关论文，不代表原文引用文献列表。

## 官方依据

- https://ngdc.cncb.ac.cn/gwh/api_documents ：GWH公开组装、项目API。
- https://ngdc.cncb.ac.cn/ascancer/js/chunk-54c27b52.0e154310.js ：当前ASCancerAtlas详情页使用的knowledge接口及参数。
- https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA000091
- https://ngdc.cncb.ac.cn/gsa/search?searchTerm=CRA015930
- https://ngdc.cncb.ac.cn/gwh/Assembly/104189/show

仅核查上述单条样例及页面脚本，没有执行用户的批量采集或历史回填。
