/**
 * 仅允许无账号信息的 HTTP(S) 外链，避免 javascript: 等危险协议。
 * 返回 null 时页面应禁用跳转入口。
 */
export function safeExternalUrl(value) {
  try {
    const u = new URL(value);
    return ["http:", "https:"].includes(u.protocol) &&
      !u.username &&
      !u.password
      ? u.href
      : null;
  } catch {
    return null;
  }
}
