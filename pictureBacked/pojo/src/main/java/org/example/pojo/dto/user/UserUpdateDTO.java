package org.example.pojo.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String userPhone;
    private String userEmail;
    private String userName;
    private String userAvatar;
    private String userProfile;
}
