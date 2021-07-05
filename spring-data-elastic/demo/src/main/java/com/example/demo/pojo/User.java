package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "sy1",createIndex = true)//这里我们设置的时一个新的索引，还没有创建，因此，可以让createIndex为true，表明会创建一个索引，默认也是true,否则false就好了
@Setting(shards = 5,replicas = 0)//原本这些信息也是可以放在@Document中设置的， 但是后来被废弃，转移位置了
public class User implements Serializable {
    @Id
    private Integer userId;
    @Field(type= FieldType.Text,analyzer = "ik_max_word")//这里指定分词，以及分词的设置
    private String userName;
    @Field(type=FieldType.Double)
    private Double deposit;
    @Field(type=FieldType.Keyword)//这里表示这个字段没必要分词，参与查询
    private String IdCard;
}
