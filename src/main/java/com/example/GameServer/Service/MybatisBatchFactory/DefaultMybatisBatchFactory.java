package com.example.GameServer.Service.MybatisBatchFactory;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultMybatisBatchFactory implements MybatisBatchFactory {
    @Override
    public <T> MybatisBatch<T> create(SqlSessionFactory sqlSessionFactory, List<T> dataList) {
        return new MybatisBatch<>(sqlSessionFactory, dataList);
    }
}