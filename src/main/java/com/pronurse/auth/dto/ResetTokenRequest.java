package com.pronurse.auth.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ResetTokenRequest {

    private String token;
}
