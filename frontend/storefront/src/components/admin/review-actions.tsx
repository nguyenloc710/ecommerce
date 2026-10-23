"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { ErrorNote } from "@/components/admin/admin-ui";
import { moderateReview, type AdminError } from "@/lib/admin-actions";
import type { ReviewStatus } from "@/lib/admin-shared";

/** Nút duyệt/từ chối một đánh giá; đã duyệt thì vẫn ẩn được (từ chối kèm lý do), đã từ chối thì duyệt lại được. */
export function ReviewModeration({ reviewId, status }: { reviewId: string; status: ReviewStatus }) {
  const router = useRouter();
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [error, setError] = useState<AdminError | null>(null);
  const [pending, start] = useTransition();

  const run = (action: "approve" | "reject") =>
    start(async () => {
      setError(null);
      const res = await moderateReview(reviewId, action, reason);
      if (!res.ok) {
        setError(res);
        return;
      }
      setRejecting(false);
      router.refresh();
    });

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        {status !== "APPROVED" ? (
          <button type="button" disabled={pending} onClick={() => run("approve")} className="btn-admin">
            Duyệt
          </button>
        ) : null}
        {status !== "REJECTED" && !rejecting ? (
          <button type="button" disabled={pending} onClick={() => setRejecting(true)} className="btn-admin btn-admin-light text-sale">
            {status === "APPROVED" ? "Ẩn đánh giá" : "Từ chối"}
          </button>
        ) : null}
      </div>
      {rejecting ? (
        <div className="flex flex-wrap items-center gap-2">
          <input
            value={reason}
            maxLength={255}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Lý do (ví dụ: quảng cáo, ngôn từ không phù hợp)"
            aria-label="Lý do từ chối"
            className="input-admin min-w-[240px] flex-1"
          />
          <button type="button" disabled={pending || !reason.trim()} onClick={() => run("reject")} className="btn-admin">
            Xác nhận
          </button>
          <button type="button" onClick={() => setRejecting(false)} className="btn-admin btn-admin-light">
            Thôi
          </button>
        </div>
      ) : null}
      {error ? <ErrorNote message={error.message} traceId={error.traceId} /> : null}
    </div>
  );
}
