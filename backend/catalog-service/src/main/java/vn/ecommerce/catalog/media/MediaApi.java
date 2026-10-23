package vn.ecommerce.catalog.media;

/** API public của module media: cấp URL upload ký sẵn và kiểm tra object key ảnh do khách tải lên. */
public interface MediaApi {

    /**
     * URL ký sẵn để trình duyệt PUT thẳng một ảnh đánh giá lên kho ảnh (R2/S3), không đi qua service.
     *
     * @throws vn.ecommerce.common.web.error.BadRequestException {@code media.type-not-allowed}, {@code media.too-large}
     */
    UploadTicket presignReviewImage(String customerId, String contentType, long sizeBytes);

    /** Key có đúng là ảnh đánh giá do chính khách này tải lên (theo tiền tố và định dạng key). */
    boolean isReviewImageOf(String customerId, String objectKey);

    /** URL công khai (CDN) của một object key. */
    String publicUrl(String objectKey);
}
