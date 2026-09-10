package org.example.identity.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Zou
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateDTO extends UserUpdateDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userRole;
    private Long id;
}
