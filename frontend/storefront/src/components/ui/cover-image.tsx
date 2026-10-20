import Image from "next/image";
import type { ReactNode } from "react";
import { PlaceholderImage, type IconName, type Tone } from "@/components/ui/placeholder-image";

type Props = {
  src?: string | null;
  alt: string;
  /** Khung ảnh: class đặt aspect-ratio / chiều cao, ví dụ "aspect-[3/4]" */
  className?: string;
  sizes: string;
  priority?: boolean;
  /** Lớp phủ tối để chữ đè lên ảnh đọc được (hero, ô chiến dịch) */
  overlay?: "none" | "bottom" | "full";
  tone?: Tone;
  icon?: IconName;
  iconSize?: number;
  children?: ReactNode;
};

const OVERLAY = {
  none: "",
  bottom: "after:absolute after:inset-x-0 after:bottom-0 after:h-1/2 after:bg-gradient-to-t after:from-black/55 after:to-transparent",
  full: "after:absolute after:inset-0 after:bg-black/20",
};

/** Ảnh phủ kín khung bằng next/image; không có ảnh thì rơi về ô placeholder cùng kích thước (UI-14). */
export function CoverImage({ src, alt, className = "", sizes, priority, overlay = "none", tone, icon, iconSize, children }: Props) {
  if (!src) {
    return (
      <PlaceholderImage tone={tone} icon={icon} iconSize={iconSize} className={className}>
        {children}
      </PlaceholderImage>
    );
  }
  return (
    <div className={`relative overflow-hidden bg-stone ${OVERLAY[overlay]} ${className}`}>
      <Image src={src} alt={alt} fill sizes={sizes} priority={priority} className="object-cover" />
      {children ? <div className="relative z-10 flex h-full w-full flex-col justify-end">{children}</div> : null}
    </div>
  );
}
