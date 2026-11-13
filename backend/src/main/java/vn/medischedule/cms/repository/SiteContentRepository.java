package vn.medischedule.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.medischedule.cms.domain.SiteContent;

public interface SiteContentRepository extends JpaRepository<SiteContent, String> {
}
