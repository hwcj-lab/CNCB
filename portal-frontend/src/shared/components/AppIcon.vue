<script setup>
defineProps({
  name: { type: String, default: "grid" },
  size: { type: Number, default: 20 },
});
const paths = {
  grid: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
  chart: "M4 3v17h17 M8 14l4-5 4 3 5-7",
  arrow: "M5 12h14 M13 6l6 6-6 6",
  external: "M14 3h7v7 M21 3l-10 10 M10 3H4v17h16v-6",
  search: "M10.5 18a7.5 7.5 0 1 0 0-15 7.5 7.5 0 0 0 0 15 M16 16l5 5",
  book: "M3 5c4-2 7-1 9 1 2-2 5-3 9-1v15c-4-2-7-1-9 1-2-2-5-3-9-1z M12 6v15",
  dna: "M6 2c0 10 12 10 12 20 M18 2C18 12 6 12 6 22 M7 5h10 M9 9h6 M9 15h6 M7 19h10",
  protein: "M5 6l7-3 7 5-2 10-9 3-5-6z M5 6l12 12 M19 8L3 15 M12 3l-4 18",
  species:
    "M12 21v-9 M12 14C3 15 2 8 3 4c7 0 10 3 9 10 M12 15c-1-7 3-10 9-10 1 6-2 10-9 10",
  database:
    "M20 5c0 2-4 3-8 3S4 7 4 5s4-3 8-3 8 1 8 3z M4 5v14c0 2 4 3 8 3s8-1 8-3V5 M4 12c0 2 4 3 8 3s8-1 8-3",
  check: "M5 12l4 4L19 6",
  info: "M12 8h.01 M12 11v6 M22 12a10 10 0 1 0-20 0 10 10 0 0 0 20 0",
  menu: "M4 6h16 M4 12h16 M4 18h16",
  layers: "M12 3L2 8l10 5 10-5z M2 12l10 5 10-5 M2 16l10 5 10-5",
};
</script>
<template>
  <svg
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.65"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path :d="paths[name] || paths.grid" />
  </svg>
</template>
<!-- 平台通用图标组件，集中维护内联 SVG 路径。 -->
