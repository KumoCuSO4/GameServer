package com.example.GameServer.Service.MybatisBatchFactory;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;

public interface MybatisBatchFactory {
    <T> MybatisBatch<T> create(SqlSessionFactory sqlSessionFactory, List<T> dataList);
}