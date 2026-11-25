import { createApp } from "vue";
import ElementPlus from "element-plus";
import zhCn from "element-plus/es/locale/lang/zh-cn";
import "element-plus/dist/index.css";
import "./assets/main.css";
import App from "./App.vue";
import router from "./router";
// 注册路由与 Element Plus 中文语言包后挂载根组件。
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount("#app");
