package vn.ecommerce.common.jpa;

import org.hibernate.envers.RevisionListener;
import vn.ecommerce.common.security.CurrentUser;

/** Điền {@code username} cho mỗi revision; Envers tự khởi tạo listener nên không inject được bean. */
public class AuditRevisionListener implements RevisionListener {

    private final CurrentUser currentUser = new CurrentUser();

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevisionEntity) revisionEntity).setUsername(currentUser.id().orElse(CurrentAuditorProvider.SYSTEM));
    }
}
