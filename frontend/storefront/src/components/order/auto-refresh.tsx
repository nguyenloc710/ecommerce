"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

/** Làm mới Server Component theo chu kỳ (chờ IPN cập nhật trạng thái đơn); dừng sau số lần tối đa. */
export function AutoRefresh({ intervalMs, maxTries }: { intervalMs: number; maxTries: number }) {
  const router = useRouter();
  const [tries, setTries] = useState(0);

  useEffect(() => {
    if (tries >= maxTries) return;
    const timer = setTimeout(() => {
      setTries((n) => n + 1);
      router.refresh();
    }, intervalMs);
    return () => clearTimeout(timer);
  }, [tries, maxTries, intervalMs, router]);

  if (tries < maxTries) return null;
  return (
    <button type="button" onClick={() => setTries(0)} className="eyebrow ul cursor-pointer">
      Kiểm tra lại
    </button>
  );
}
