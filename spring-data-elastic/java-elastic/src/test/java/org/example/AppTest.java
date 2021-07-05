package org.example;

import static org.junit.Assert.assertTrue;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.naming.directory.SearchResult;
import javax.swing.text.Highlighter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AppTest 
{
    private RestHighLevelClient restHighLevelClient=null;
    private final String SCHEME="HTTP";
    HttpHost[] httpHosts=new HttpHost[]{
            new HttpHost("192.168.43.182",9200,SCHEME)};

    @Before
    public void init(){
        restHighLevelClient=new RestHighLevelClient(
                RestClient.builder(httpHosts));
    }
    @After
    public void close() {
        if(restHighLevelClient!=null){
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //添加
    @Test
    public void create(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","他的名字");
        map.put("content","他写的内容");
        IndexRequest indexRequest=new IndexRequest("sy").id("1").source(map);
        try {
            IndexResponse response =restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //查询
    @Test
    public void query(){
        GetRequest getRequest=new GetRequest("sy","1");
        try {
            GetResponse getResponse=restHighLevelClient.get(getRequest,RequestOptions.DEFAULT);
            System.out.println(getResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //更新
    @Test
    public void update(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","他的新名字");
        map.put("content","他写的新内容");
        UpdateRequest updateRequest=new UpdateRequest("sy","1").doc(map);
        try {
            UpdateResponse updateResponse=restHighLevelClient.update(updateRequest,
                    RequestOptions.DEFAULT);
            System.out.println(updateRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void delete(){
        DeleteRequest deleteRequest=new DeleteRequest("sy","f1ElcHoBil8eT6B31mpv");
        try {
            DeleteResponse deleteResponse=restHighLevelClient.delete(deleteRequest,
                    RequestOptions.DEFAULT);
            System.out.println(deleteResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //批量操作
    @Test
    public void batch(){
        BulkRequest bulkRequest=new BulkRequest();
        bulkRequest.add(new IndexRequest("sy").id("3").source(XContentType.JSON,
                "name","我的名字","content","别人的名字"));
        bulkRequest.add(new UpdateRequest("sy","3").doc(XContentType.JSON,
                "name","我的新名字"));
        bulkRequest.add(new DeleteRequest("sy","2"));
        try {
            BulkResponse bulkResponse= restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
            System.out.println(bulkResponse.toString()  );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //高级查询
    @Test
    public void testquery(){
        SearchRequest searchRequest = new SearchRequest("sy");//可以指定多个索引库
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                                                query(QueryBuilders.matchAllQuery());//指定的查询是匹配所有
        //SearchSourceBuilder默认最多显示10条结果，可以通过size()设置，也可以通过from()设置从哪个索引开始搜索
        //也可以用sort()指定排序，是按照分数或其它，这些排序的依据可以使用类sortBuilders指定,sortBuilder可以指定是正序或是倒序,
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            long value = searchResponse.getHits().getTotalHits().value;
            System.out.println("记录总数"+value);
            if(value>0){
                SearchHit[] hits=searchResponse.getHits().getHits();
                for(SearchHit hit:hits){
                    System.out.println("index"+hit.getIndex()+", id"+hit.getId());
                    System.out.println("name"+hit.getSourceAsMap().get("name")+
                                       ", content"+hit.getSourceAsMap().get("content"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testquerymatch(){
        SearchRequest searchRequest = new SearchRequest("sy");//可以指定多个索引库
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                query(QueryBuilders.multiMatchQuery("别人","name","content"));//指定的查询是匹配所有
        HighlightBuilder highlightBuilder=new HighlightBuilder().field("content").preTags("<font color=yellow>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);//设置高亮
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            long value = searchResponse.getHits().getTotalHits().value;
            System.out.println("记录总数"+value);
            if(value>0){
                SearchHit[] hits=searchResponse.getHits().getHits();
                for(SearchHit hit:hits){
                    System.out.println("index"+hit.getIndex()+", id"+hit.getId());
                    System.out.println("name"+hit.getSourceAsMap().get("name")  +
                            ", content"+String.valueOf(hit.getHighlightFields().get("content").fragments()[0]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
