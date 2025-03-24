package com.example.GameServer.PO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("player_item")
public class PlayerItemPO {
    private Long uid;

    private Integer itemId;

    private Integer num;
}
