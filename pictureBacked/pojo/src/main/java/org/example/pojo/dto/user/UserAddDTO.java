package org.example.pojo.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserAddDTO implements Serializable {
    @Serial
    private final static long serialVersionUID = 1L;

    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;

}
