import { logout } from "@/lib/auth-actions";

export function LogoutButton() {
  return (
    <form action={logout}>
      <button type="submit" className="eyebrow ul cursor-pointer">
        Đăng xuất
      </button>
    </form>
  );
}
