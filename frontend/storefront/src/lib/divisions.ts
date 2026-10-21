import { cacheLife } from "next/cache";
import { apiFetch } from "@/lib/api";
import type { Division } from "@/lib/checkout-shared";

/** Danh mục tỉnh/phường (B-13) gần như không đổi: cache theo ngày. API công khai, không cần token. */
export async function getProvinces(): Promise<Division[]> {
  "use cache";
  cacheLife("days");
  const list = await apiFetch<Division[]>("/api/divisions/provinces");
  return list.map(({ code, name }) => ({ code, name }));
}

export async function getWards(provinceCode: string): Promise<Division[]> {
  "use cache";
  cacheLife("days");
  const list = await apiFetch<Division[]>(`/api/divisions/provinces/${encodeURIComponent(provinceCode)}/wards`);
  return list.map(({ code, name }) => ({ code, name }));
}
