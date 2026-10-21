import type { Metadata } from "next";
import { Suspense } from "react";
import { redirect } from "next/navigation";
import { login } from "@/lib/auth-actions";
import { safeCallbackUrl } from "@/lib/callback-url";
import { getSession } from "@/lib/session";

export const metadata: Metadata = { title: "Đăng nhập", robots: { index: false } };

/** Mã lỗi Auth.js gửi qua ?error= (pages.error trỏ về đây). */
const ERROR_MESSAGES: Record<string, string> = {
  AccessDenied: "Tài khoản không được phép đăng nhập.",
  Configuration: "Hệ thống đăng nhập đang gặp sự cố, vui lòng thử lại sau.",
};

export default function Page({ searchParams }: PageProps<"/dang-nhap">) {
  return (
    <main className="pad flex flex-1 items-center justify-center py-20 lg:py-32">
      <div className="w-full max-w-[420px] text-center">
        <span className="eyebrow text-muted">Tài khoản</span>
        <h1 className="font-display mt-3 text-[40px] font-medium leading-none lg:text-[48px]">Đăng nhập</h1>
        <p className="mx-auto mt-4 max-w-[340px] text-[14px] text-muted">
          Lưu địa chỉ giao hàng, theo dõi đơn và danh sách yêu thích. Chưa có tài khoản? Bạn có thể đăng ký ở bước tiếp theo.
        </p>
        <Suspense fallback={<div className="mt-10 h-[52px]" />}>
          <LoginForm searchParams={searchParams} />
        </Suspense>
      </div>
    </main>
  );
}

async function LoginForm({ searchParams }: Pick<PageProps<"/dang-nhap">, "searchParams">) {
  const params = await searchParams;
  const callbackUrl = safeCallbackUrl(params.callbackUrl);
  // Đã đăng nhập (ví dụ bấm Back sau khi đăng nhập) thì đi tiếp luôn
  if (await getSession()) redirect(callbackUrl);

  const error = typeof params.error === "string" ? (ERROR_MESSAGES[params.error] ?? "Đăng nhập không thành công, vui lòng thử lại.") : null;
  return (
    <form action={login} className="mt-10 flex flex-col gap-4">
      {error && (
        <p role="alert" className="m-0 border border-sale px-4 py-3 text-[13px] text-sale">
          {error}
        </p>
      )}
      <input type="hidden" name="callbackUrl" value={callbackUrl} />
      <button type="submit" className="btn w-full">
        Đăng nhập / Đăng ký
      </button>
    </form>
  );
}
