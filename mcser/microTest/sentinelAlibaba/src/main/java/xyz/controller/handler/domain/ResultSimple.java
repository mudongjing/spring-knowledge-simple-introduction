package xyz.controller.handler.domain;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultSimple<T> {
    private Integer codes;
    private String msg;
    private T data;
    public ResultSimple(Integer codes,String msg){
        this.codes=codes;
        this.msg=msg;
        this.data=null;
    }
    public static ResultSimple err(Integer codes,String msg){
        return new ResultSimple(codes,msg);
    }
}
