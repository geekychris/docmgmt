package com.docmgmt.service;

import com.docmgmt.model.SysObject;
import com.docmgmt.repository.SysObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service implementation for base SysObject entities
 */
@Service
public class SysObjectService extends AbstractSysObjectService<SysObject, SysObjectRepository> {

    /**
     * Constructor with repository injection
     * @param repository The SysObject repository
     */
    @Autowired
    public SysObjectService(SysObjectRepository repository) {
        super(repository);
    }
    
    // Additional service methods can be added here if needed
}

