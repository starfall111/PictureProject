package org.example.identity.interfaces.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserPasswordUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
