package com.example.demo.dao;

import com.example.demo.pojo.User;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository//这里就类似以前对于mybatis的操作，定义接口，内部的方法对应着不同的操作语句
public interface UserRepository extends ElasticsearchRepository<User,Integer> {
    //直接用注解说明方法的作用了，内部使用的ES的语法
    @Query("{\"match:\":{\"userId\":{\"query\":\"?0\"}}}")
    User findByUserId(Integer id);
}
