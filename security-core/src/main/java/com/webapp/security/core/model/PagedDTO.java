package com.webapp.security.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PagedDTO extends BasePaged {
    private String keyword;
}
