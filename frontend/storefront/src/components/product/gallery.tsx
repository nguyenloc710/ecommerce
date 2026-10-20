import Image from "next/image";
import { PlaceholderImage, type IconName } from "@/components/ui/placeholder-image";

type Img = { id: string; url: string; alt: string | null };

/**
 * Gallery PDP (UI-09): desktop 2 cột xếp dọc cuộn theo trang; mobile cuộn ngang bắt dính từng ảnh.
 */
export function Gallery({ images, name, icon }: { images: Img[]; name: string; icon?: IconName }) {
  if (images.length === 0) {
    return <PlaceholderImage tone="stone" icon={icon} iconSize={180} className="aspect-[3/4] lg:aspect-auto lg:h-[90vh]" />;
  }
  return (
    <>
      {/* Mobile */}
      <div className="flex snap-x snap-mandatory overflow-x-auto lg:hidden" aria-label={`Ảnh ${name}`}>
        {images.map((img, i) => (
          <div key={img.id} className="relative aspect-[4/5] w-full flex-none snap-center bg-stone">
            <Image src={img.url} alt={img.alt ?? `${name} ${i + 1}`} fill sizes="100vw" priority={i === 0} className="object-cover" />
            <span className="absolute bottom-3 left-3 text-[10px] uppercase tracking-[0.14em] text-ink/60">
              {i + 1} / {images.length}
            </span>
          </div>
        ))}
      </div>
      {/* Desktop */}
      <div className={`hidden gap-1 p-1 lg:grid ${images.length === 1 ? "grid-cols-1" : "grid-cols-2"}`}>
        {images.map((img, i) => (
          <div key={img.id} className="relative aspect-[3/4] bg-stone">
            <Image src={img.url} alt={img.alt ?? `${name} ${i + 1}`} fill sizes="(min-width: 1024px) 30vw, 100vw" priority={i < 2} className="object-cover" />
          </div>
        ))}
      </div>
    </>
  );
}
