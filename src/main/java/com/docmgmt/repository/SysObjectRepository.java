package com.docmgmt.repository;

import com.docmgmt.model.SysObject;
import org.springframework.stereotype.Repository;

/**
 * Repository for base SysObject entities
 */
@Repository
public interface SysObjectRepository extends BaseSysObjectRepository<SysObject> {
    // Additional repository methods can be added here if needed
}
