package com.zr.bili.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    private Long id;

    private String username;

    private String password;

    private String email;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String avatar;
}
