import { revalidateTag } from "next/cache";
import { NextResponse } from "next/server";
import { CATALOG_CACHE_TAG } from "@/lib/catalog";

/**
 * Admin sửa sản phẩm xong gọi POST /api/revalidate với header X-Revalidate-Secret để storefront làm mới cache
 * catalog ngay (stale-while-revalidate), không phải chờ hết cacheLife.
 */
export async function POST(request: Request) {
  const secret = process.env.REVALIDATE_SECRET;
  if (!secret || request.headers.get("x-revalidate-secret") !== secret) {
    return NextResponse.json({ code: "common.access-denied" }, { status: 403 });
  }
  revalidateTag(CATALOG_CACHE_TAG, "max");
  return NextResponse.json({ revalidated: true, tag: CATALOG_CACHE_TAG });
}
