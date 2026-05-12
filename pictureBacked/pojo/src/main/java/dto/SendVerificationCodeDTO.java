package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendVerityCodeDTO {
    private Integer type;
    private String account;
    private String captchaVerifyParam;
}
