package com.webapp.security.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BasePaged {
    private int pageNum = 1;
    private int pageSize = 10;
}
