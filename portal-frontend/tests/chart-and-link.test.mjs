import test from "node:test";
import assert from "node:assert/strict";
import { safeExternalUrl } from "../src/shared/utils/links.js";
import {
  annualOption,
  distributionOption,
  rankingOption,
} from "../src/modules/research/utils/chartOptions.mjs";
test("external links reject scripts, synthetic sources and embedded credentials", () => {
  for (const link of [
    "javascript:alert(1)",
    "DEMO:synthetic",
    "data:text/html,x",
    "https://user:pass@example.com",
    "/relative",
  ])
    assert.equal(safeExternalUrl(link), null);
  assert.equal(
    safeExternalUrl("https://ngdc.cncb.ac.cn/bit/needle"),
    "https://ngdc.cncb.ac.cn/bit/needle",
  );
});
test("annual chart fills only registered-sample year gaps without changing counts", () => {
  const option = annualOption([
    { year: 2020, value: 2, dimensionId: 10 },
    { year: 2022, value: 1, dimensionId: 12 },
  ]);
  assert.deepEqual(option.xAxis.data, ["2020", "2021", "2022"]);
  assert.deepEqual(option.series[0].data.map((r) => r.value), [2, 0, 1]);
  assert.deepEqual(option.series[0].data.map((r) => r.dimensionId), [10, undefined, 12]);
  assert.equal(
    option.series[0].data.reduce((a, b) => a + b.value, 0),
    3,
  );
  assert.deepEqual(annualOption([]).series[0].data, []);
});
test("multi-label distribution keeps actual counts rather than forcing paper-total percentages", () => {
  const option = distributionOption([
    { name: "项目", value: 5 },
    { name: "样本", value: 4 },
  ]);
  assert.equal(
    option.series[0].data.reduce((s, r) => s + r.value, 0),
    9,
  );
  assert.equal(option.tooltip.renderMode, "richText");
});
test("ranking reverses copies, retains identity names and limits display", () => {
  const rows = [
    { id: "1", name: "演示机构 A", value: 3 },
    { id: "2", name: "演示机构 B", value: 2 },
  ];
  const option = rankingOption(rows);
  assert.equal(rows[0].id, "1");
  assert.deepEqual(option.yAxis.data, ["演示机构 B", "演示机构 A"]);
  assert.equal(option.series[0].data[1].value, 3);
});
