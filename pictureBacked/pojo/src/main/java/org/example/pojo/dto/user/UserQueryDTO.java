package org.example.pojo.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.pojo.PageRequest;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryDTO extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String userAccount;
    private String userPhone;
    private String userEmail;
    private String userName;
    private String userRole;


}
