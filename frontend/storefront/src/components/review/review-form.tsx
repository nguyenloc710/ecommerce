"use client";

import Link from "next/link";
import { useEffect, useRef, useState, useTransition } from "react";
import { getReviewEligibility, requestReviewUpload, submitReview } from "@/lib/review-actions";
import {
  REVIEW_IMAGE_TYPES,
  REVIEW_MAX_CONTENT,
  REVIEW_MAX_IMAGE_BYTES,
  REVIEW_MAX_IMAGES,
  type Eligibility,
} from "@/lib/reviews-shared";

type Picked = { file: File; preview: string };

/**
 * Form đánh giá (B-10): chỉ hiện khi khách đã nhận hàng. Ảnh upload thẳng từ trình duyệt lên kho ảnh bằng URL ký sẵn,
 * sau đó gửi đánh giá kèm object key. Đánh giá chờ duyệt nên không hiện ngay.
 */
export function ReviewForm({ productId, productSlug }: { productId: string; productSlug: string }) {
  const [eligibility, setEligibility] = useState<Eligibility | null>(null);
  const [rating, setRating] = useState(0);
  const [hover, setHover] = useState(0);
  const [content, setContent] = useState("");
  const [images, setImages] = useState<Picked[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [pending, start] = useTransition();
  // Giữ danh sách mới nhất để giải phóng URL xem trước khi rời trang (không giải phóng ảnh còn đang hiện)
  const imagesRef = useRef<Picked[]>([]);
  useEffect(() => {
    imagesRef.current = images;
  }, [images]);
  useEffect(() => () => imagesRef.current.forEach((i) => URL.revokeObjectURL(i.preview)), []);

  useEffect(() => {
    let alive = true;
    void getReviewEligibility(productId).then((e) => alive && setEligibility(e));
    return () => {
      alive = false;
    };
  }, [productId]);

  if (!eligibility) return null;
  if (done) {
    return <p className="m-0 border border-line px-4 py-3 text-[13px]">Cảm ơn bạn! Đánh giá sẽ hiển thị sau khi cửa hàng duyệt.</p>;
  }
  if (eligibility.reason === "LOGIN_REQUIRED") {
    return (
      <p className="m-0 text-[13px] text-muted">
        Đã mua sản phẩm này?{" "}
        <Link href={`/dang-nhap?callbackUrl=${encodeURIComponent(`/san-pham/${productSlug}#danh-gia`)}`} className="ul text-ink">
          Đăng nhập để đánh giá
        </Link>
      </p>
    );
  }
  if (eligibility.reason === "ALREADY_REVIEWED") return <p className="m-0 text-[13px] text-muted">Bạn đã đánh giá sản phẩm này.</p>;
  if (!eligibility.canReview) return <p className="m-0 text-[13px] text-muted">Chỉ khách đã mua và nhận hàng mới đánh giá được sản phẩm.</p>;

  const pick = (files: FileList | null) => {
    setError(null);
    const next = [...images];
    for (const file of Array.from(files ?? [])) {
      if (next.length >= REVIEW_MAX_IMAGES) {
        setError(`Tối đa ${REVIEW_MAX_IMAGES} ảnh.`);
        break;
      }
      if (!REVIEW_IMAGE_TYPES.includes(file.type)) {
        setError("Chỉ nhận ảnh JPG, PNG hoặc WEBP.");
        continue;
      }
      if (file.size > REVIEW_MAX_IMAGE_BYTES) {
        setError("Mỗi ảnh tối đa 5 MB.");
        continue;
      }
      next.push({ file, preview: URL.createObjectURL(file) });
    }
    setImages(next);
  };

  const submit = () =>
    start(async () => {
      setError(null);
      if (rating === 0) {
        setError("Vui lòng chọn số sao.");
        return;
      }
      const keys: string[] = [];
      for (const { file } of images) {
        const ticket = await requestReviewUpload(file.type, file.size);
        if (!ticket.ok) {
          setError(ticket.message);
          return;
        }
        const put = await fetch(ticket.uploadUrl, { method: "PUT", headers: ticket.headers, body: file }).catch(() => null);
        if (!put?.ok) {
          setError("Tải ảnh lên không thành công, vui lòng thử lại.");
          return;
        }
        keys.push(ticket.objectKey);
      }
      const res = await submitReview({ productId, rating, content, imageKeys: keys });
      if (!res.ok) {
        setError(res.traceId ? `${res.message} (mã lỗi ${res.traceId})` : res.message);
        return;
      }
      setDone(true);
    });

  const shown = hover || rating;
  return (
    <div className="flex flex-col gap-4 border-t border-line pt-6">
      <span className="eyebrow">Viết đánh giá</span>
      <div role="radiogroup" aria-label="Số sao" className="flex gap-1" onMouseLeave={() => setHover(0)}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            role="radio"
            aria-checked={rating === star}
            aria-label={`${star} sao`}
            onMouseEnter={() => setHover(star)}
            onClick={() => setRating(star)}
            className={`text-[26px] leading-none ${star <= shown ? "text-ink" : "text-line"}`}
          >
            ★
          </button>
        ))}
      </div>
      <label className="flex flex-col gap-1.5 text-[12px] text-muted">
        Cảm nhận của bạn (không bắt buộc)
        <textarea
          rows={4}
          maxLength={REVIEW_MAX_CONTENT}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          className="input py-3 text-[14px]"
        />
      </label>
      <div className="flex flex-wrap items-center gap-2">
        {images.map((img, i) => (
          <div key={img.preview} className="relative h-16 w-16 bg-stone">
            {/* Ảnh xem trước là blob: cục bộ, next/image không áp dụng */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={img.preview} alt={`Ảnh ${i + 1}`} className="h-full w-full object-cover" />
            <button
              type="button"
              aria-label={`Bỏ ảnh ${i + 1}`}
              onClick={() => {
                URL.revokeObjectURL(img.preview);
                setImages(images.filter((x) => x !== img));
              }}
              className="absolute -right-2 -top-2 inline-flex h-5 w-5 items-center justify-center rounded-full bg-ink text-[11px] text-paper"
            >
              ×
            </button>
          </div>
        ))}
        {images.length < REVIEW_MAX_IMAGES ? (
          <label className="inline-flex h-16 w-16 cursor-pointer items-center justify-center border border-dashed border-line text-[11px] text-muted hover:border-ink">
            + Ảnh
            <input type="file" accept={REVIEW_IMAGE_TYPES.join(",")} multiple className="sr-only" onChange={(e) => pick(e.target.files)} />
          </label>
        ) : null}
      </div>
      {error ? (
        <p role="alert" className="m-0 text-[12px] text-sale">
          {error}
        </p>
      ) : null}
      <button type="button" disabled={pending} onClick={submit} className="btn w-full disabled:opacity-60">
        {pending ? "Đang gửi…" : "Gửi đánh giá"}
      </button>
    </div>
  );
}
