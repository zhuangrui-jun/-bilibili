package com.zr.bili.entity.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String token;
}
