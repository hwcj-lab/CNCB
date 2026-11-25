const palette = [
  "#3f9f85",
  "#83b9a1",
  "#6d9fa7",
  "#a6b98d",
  "#94a5bc",
  "#c5b58a",
  "#96c9bf",
  "#b4c9a0",
];
const tooltip = {
  trigger: "axis",
  renderMode: "richText",
  backgroundColor: "#fff",
  borderColor: "#e1e9e5",
  textStyle: { color: "#597c74", fontSize: 13 },
  padding: 12,
};
export function annualOption(items, resourceMode = false) {
  const entries = new Map(items.map((r) => [Number(r.year), r]));
  const observed = [...entries.keys()]
    .filter(Number.isFinite)
    .sort((a, b) => a - b);
  const years = observed.length
    ? Array.from(
        { length: Math.min(701, observed.at(-1) - observed[0] + 1) },
        (_, i) => observed[0] + i,
      )
    : [];
  return {
    animationDuration: 350,
    color: palette,
    tooltip,
    grid: { left: 42, right: 22, top: 38, bottom: 32 },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: years.map(String),
      axisLine: { lineStyle: { color: "#e2ebe7" } },
      axisTick: { show: false },
      axisLabel: { color: "#96a7a8", fontSize: 13 },
    },
    yAxis: {
      type: "value",
      minInterval: 1,
      name: resourceMode ? "统计对象 / 条" : "已收录文献 / 篇",
      nameTextStyle: { color: "#a7b3b3", fontSize: 12 },
      splitLine: { lineStyle: { color: "#edf2ef", type: "dashed" } },
      axisLabel: { color: "#9babad", fontSize: 13 },
    },
    series: [
      {
        name: resourceMode ? "统计对象数" : "文献数量",
        type: "line",
        smooth: false,
        symbol: "circle",
        symbolSize: 5,
        data: years.map((y) => {
          const item = entries.get(y);
          return {
            value: Number(item?.value || 0),
            dimensionId: item?.dimensionId,
            name: String(y),
          };
        }),
        lineStyle: { width: 2.5 },
        itemStyle: { color: "#37977d", borderColor: "#fff", borderWidth: 2 },
        areaStyle: {
          color: {
            type: "linear",
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: "#65b49b40" },
              { offset: 1, color: "#65b49b02" },
            ],
          },
        },
      },
    ],
  };
}
export function distributionOption(items, resourceMode = false) {
  const counts = new Map(items.map((item) => [item.name, Number(item.value) || 0]));
  return {
    animationDuration: 350,
    color: palette,
    tooltip: {
      trigger: "item",
      renderMode: "richText",
      formatter: `{b}\n{c} ${resourceMode ? "条" : "篇"} · 关联计数占比 {d}%`,
      textStyle: { fontSize: 13 },
    },
    legend: {
      type: "scroll",
      bottom: 4,
      left: "center",
      icon: "circle",
      itemWidth: 7,
      itemHeight: 7,
      itemGap: 13,
      textStyle: { color: "#8b9f9d", fontSize: 13 },
      formatter: (name) => `${name} ${counts.get(name) ?? 0}`,
    },
    series: [
      {
        name: "研究主题",
        type: "pie",
        radius: ["48%", "68%"],
        center: ["50%", "43%"],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: "#fff", borderWidth: 4, borderRadius: 4 },
        label: { show: false },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            color: "#587d70",
            formatter: `{b}\n{c} ${resourceMode ? "条" : "篇"}`,
          },
        },
        data: items.map((i) => ({
          name: i.name,
          value: Number(i.value),
          dimensionId: i.dimensionId,
          dimensionKey: i.dimensionKey,
        })),
      },
    ],
  };
}
export function rankingOption(items, resourceMode = false) {
  const rows = [...items].slice(0, 50).reverse();
  const zoomStart = Math.max(0, 100 - (12 / Math.max(rows.length, 1)) * 100);
  return {
    animationDuration: 350,
    color: palette,
    tooltip: { ...tooltip, axisPointer: { type: "shadow" } },
    grid: { left: 123, right: rows.length > 12 ? 52 : 33, top: 22, bottom: 26 },
    xAxis: {
      type: "value",
      minInterval: 1,
      axisLabel: { color: "#a3b1b1", fontSize: 12 },
      splitLine: { lineStyle: { color: "#eef2f0", type: "dashed" } },
    },
    yAxis: {
      type: "category",
      data: rows.map((r) => r.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: {
        color: "#7b9690",
        fontSize: 13,
        width: 112,
        overflow: "truncate",
      },
    },
    dataZoom: rows.length > 12 ? [
      { type: "inside", yAxisIndex: 0, start: zoomStart, end: 100, zoomOnMouseWheel: true, moveOnMouseWheel: true },
      {
        type: "slider", yAxisIndex: 0, start: zoomStart, end: 100, right: 7, width: 13,
        showDetail: false, showDataShadow: false, borderColor: "transparent",
        backgroundColor: "#18201e", fillerColor: "#477f6d66",
        handleSize: 14, handleStyle: { color: "#70ab98", borderColor: "#70ab98" },
      },
    ] : [],
    series: [
      {
        name: resourceMode ? "关联记录" : "关联文献",
        type: "bar",
        barMaxWidth: 14,
        data: rows.map((r, i) => ({
          value: Number(r.value),
          dimensionId: r.dimensionId,
          name: r.name,
          itemStyle: {
            color: i === rows.length - 1 ? "#489d82" : "#add1be",
            borderRadius: [0, 3, 3, 0],
          },
        })),
        label: {
          show: true,
          position: "right",
          fontSize: 13,
          color: "#8aa59a",
        },
      },
    ],
  };
}
