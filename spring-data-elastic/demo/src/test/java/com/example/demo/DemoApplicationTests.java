package com.example.demo;

import com.example.demo.dao.UserRepository;
import com.example.demo.pojo.User;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class DemoApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate  elasticsearchRestTemplate;
    @Autowired
    private UserRepository userRepository;
    @Test
    public void Index(){
        IndexOperations indexOperations=elasticsearchRestTemplate.indexOps(User.class);
        indexOperations.create();//创建索引
        Document document=indexOperations.createMapping(User.class);
        indexOperations.putMapping(document);//创造的映射与我们的实体类绑定
    }
    @Test
    public void testRepository(){
        User user=userRepository.findByUserId(1);//使用我们自定义的方法
        System.out.println(user);
    }
    @Test
    public void testRest(){
        List<User> list=new ArrayList<>();
        list.add(new User(1,"我的名字",56.03,"23456"));
        list.add(new User(2,"又一个名字",57.05,"34567"));
        list.add(new User(3,"我的又一个名字",59.07,"45678"));
        elasticsearchRestTemplate.save(list);//添加记录，如果对应的id存在，就是更新
        //删除之类的方法也可自己尝试
        //elasticsearchRestTemplate可以进行增删改查的方法基本来自接口DocumentOperations

        //下面介绍搜索
        NativeSearchQueryBuilder nativeSearchQueryBuilder=new NativeSearchQueryBuilder();
        NativeSearchQuery query=nativeSearchQueryBuilder.withQuery(
                QueryBuilders.multiMatchQuery("我的","userName")).build();
        //”我的“对应是查询内容，后面的”userName"指定查询的字段
        SearchHits<User> searchHits = elasticsearchRestTemplate.search(query, User.class);
        //返回的是搜索结果
        searchHits.forEach(System.out::println);//这是循环输出的一个lambda表达方式
        //上述的query指定了我们要查询的条件，如果删除也要指定特定的内容，也可以使用上述的那个query
    }

    //查询结果分页排序，主要就是添加个withPageable，withSort之类的
    @Test
    public void pagesortquery(){
        NativeSearchQueryBuilder nativeSearchQueryBuilder=new NativeSearchQueryBuilder();
        NativeSearchQuery query=nativeSearchQueryBuilder.withQuery(
                QueryBuilders.multiMatchQuery("我的","userName"))
                .withPageable(PageRequest.of(0,5))//指明第几页，前几个
                .withSort(SortBuilders.scoreSort().order(SortOrder.ASC))//指定按分数排序，正序排列
                //.withSort(SortBuilders.fieldSort("指定的字段名")) //如果需要按某个字段的大小排列，排序默认是倒序的
                //上述值针对或分页或排序的，如果我全都要，也有简约的
                //.withPageable(PageRequest.of(0,5, Sort.Direction.ASC,"deposit"))
                //上述的语句合并了分页和排序，这里指定是按照用户的deposit字段排，后面还可以写其它字段，将依次进行排序
                //最后说明一下高亮操作,默认高亮操作是斜体
                //.withHighlightFields(new HighlightBuilder.Field("userName"))
                .withHighlightBuilder(new HighlightBuilder().field("userName").preTags("<font color=yellow>").postTags("</font>"))//自定义高亮格式
                .build();
        SearchHits<User> searchHits = elasticsearchRestTemplate.search(query, User.class);
        for (SearchHit<User> userSearchHit : searchHits){
            System.out.println("userId" + userSearchHit.getId());
            System.out.println("score" + userSearchHit.getScore());
            System.out.println("scoreValues" + userSearchHit.getSortValues());
            System.out.println("结果内容content" + userSearchHit.getContent());//内部包含所有结果对应的文档信息
            List<String> username=userSearchHit.getHighlightField("userName");//提取高亮的内容
            username.forEach(System.out::println);
        }
    }
}
