package com.example.GameServer.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
@TableName("user")
public class UserPO {
    @Id
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
}
