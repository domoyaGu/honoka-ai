/**
 * Token 本地存取（对照 jiayou `composable/auth`）
 * 阶段 A 只用 access token；refresh token 等阶段 B 再补。
 */
const TOKEN_KEY = "honoka_token";

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || "";
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY);
}
