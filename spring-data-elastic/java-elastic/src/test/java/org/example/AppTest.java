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

    //??????
    @Test
    public void create(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","????????????");
        map.put("content","???????????????");
        IndexRequest indexRequest=new IndexRequest("sy").id("1").source(map);
        try {
            IndexResponse response =restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //??????
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

    //??????
    @Test
    public void update(){
        Map<String ,Object> map=new HashMap<>();
        map.put("name","???????????????");
        map.put("content","??????????????????");
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

    //????????????
    @Test
    public void batch(){
        BulkRequest bulkRequest=new BulkRequest();
        bulkRequest.add(new IndexRequest("sy").id("3").source(XContentType.JSON,
                "name","????????????","content","???????????????"));
        bulkRequest.add(new UpdateRequest("sy","3").doc(XContentType.JSON,
                "name","???????????????"));
        bulkRequest.add(new DeleteRequest("sy","2"));
        try {
            BulkResponse bulkResponse= restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
            System.out.println(bulkResponse.toString()  );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //????????????
    @Test
    public void testquery(){
        SearchRequest searchRequest = new SearchRequest("sy");//???????????????????????????
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                                                query(QueryBuilders.matchAllQuery());//??????????????????????????????
        //SearchSourceBuilder??????????????????10????????????????????????size()????????????????????????from()?????????????????????????????????
        //????????????sort()??????????????????????????????????????????????????????????????????????????????sortBuilders??????,sortBuilder?????????????????????????????????,
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            long value = searchResponse.getHits().getTotalHits().value;
            System.out.println("????????????"+value);
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
        SearchRequest searchRequest = new SearchRequest("sy");//???????????????????????????
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder().
                query(QueryBuilders.multiMatchQuery("??????","name","content"));//??????????????????????????????
        HighlightBuilder highlightBuilder=new HighlightBuilder().field("content").preTags("<font color=yellow>").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);//????????????
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
            long value = searchResponse.getHits().getTotalHits().value;
            System.out.println("????????????"+value);
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
