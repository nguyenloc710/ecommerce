"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const pad = (n: number) => String(n).padStart(2, "0");

/** "02:14:05" hoặc "1 ngày 02:14:05" từ số mili giây còn lại. */
function format(ms: number) {
  const total = Math.max(0, Math.floor(ms / 1000));
  const days = Math.floor(total / 86400);
  const clock = `${pad(Math.floor((total % 86400) / 3600))}:${pad(Math.floor((total % 3600) / 60))}:${pad(total % 60)}`;
  return days > 0 ? `${days} ngày ${clock}` : clock;
}

/**
 * Đếm ngược tới {@code endsAt} (ISO). Hết giờ thì làm mới trang để lấy lại giá thường từ server.
 * Render lần đầu ở server chưa biết giờ máy khách nên để trống tới khi mount (tránh lệch hydrate).
 */
export function Countdown({ endsAt, className }: { endsAt: string; className?: string }) {
  const router = useRouter();
  const end = new Date(endsAt).getTime();
  const [left, setLeft] = useState<number | null>(null);

  useEffect(() => {
    const tick = () => {
      const remaining = end - Date.now();
      setLeft(remaining);
      if (remaining <= 0) {
        clearInterval(timer);
        router.refresh();
      }
    };
    const timer = setInterval(tick, 1000);
    tick();
    return () => clearInterval(timer);
  }, [end, router]);

  return (
    <span className={`tabular-nums ${className ?? ""}`} suppressHydrationWarning>
      {left === null ? "--:--:--" : format(left)}
    </span>
  );
}
