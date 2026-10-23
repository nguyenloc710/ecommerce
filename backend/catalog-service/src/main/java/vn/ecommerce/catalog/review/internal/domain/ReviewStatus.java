package vn.ecommerce.catalog.review.internal.domain;

/** Trạng thái kiểm duyệt; khách tạo ra PENDING, nhân viên chuyển qua lại APPROVED/REJECTED. */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
