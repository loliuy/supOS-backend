package com.supos.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDto {


    private String policyId;

    private String resourceId;

    private String uri;

    private List<String> methods = new ArrayList<>();



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceDto myObject = (ResourceDto) o;
        return uri.equals(myObject.uri);
    }

}
