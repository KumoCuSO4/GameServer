package com.example.GameServer.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`user`")
public class UserPO {
    private Integer id;
    private String name;
}
