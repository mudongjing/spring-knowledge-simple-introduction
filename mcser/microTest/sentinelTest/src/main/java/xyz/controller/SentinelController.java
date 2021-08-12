package xyz.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.pojo.Handler.FallbackHandler;
import xyz.pojo.User;
import xyz.pojo.Handler.BlockHandler;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class SentinelController {
    private static final String RESOURCE_NAME="hello";
    private static final String USER_RESOURCE_NAME="user";
    private static final String DEGRADE_RESOURCE_NAME="degrade";


    @RequestMapping("/hello")
    public String hello(){

        Entry entry=null;
        try{
            //定义资源名称
            entry = SphU.entry(RESOURCE_NAME);
            String str="hello world";
            log.info("==="+str+"===");
            return str;
        }catch(Exception e){
            log.info("block!");
            Tracer.traceEntry(e,entry);
            return "被流控";
        }finally {
            if(entry!=null){
                entry.exit();
            }
        }
    }

    @PostConstruct //在创建该类时，会自动运行该方法，进行初始化
    private static void initRules(){
        //流控规则列表
        List<FlowRule> rules=new ArrayList<>();
        //流控
        FlowRule rule=new FlowRule();
        //设置受保护的资源
        rule.setResource(USER_RESOURCE_NAME);
        //设置规则
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(1);
        rules.add(rule);


        //加载配置的规则
        FlowRuleManager.loadRules(rules);
    }

    @RequestMapping("/user")
    //value设置资源
    //blockHandler 设置流控降级后的处理方法,【默认】该方法必须一起声明在该类中
        //如果该方法是放在其它类中，则需要用blockHandlerClass =指明那个类，
        //需要注意的是，对应的方法需要是static，不然反射是无法获取到
    //fallback 同样指定一个方法，可用于当接口异常时调用
        // exceptionsToIgnore 用于排除一些异常处理
    @SentinelResource(
            value=USER_RESOURCE_NAME,
            blockHandler = "blockHandlerForGetUser",
            blockHandlerClass = BlockHandler.class,
            fallback = "fallbackHandlerForGetUser",
            fallbackClass = FallbackHandler.class,
            exceptionsToIgnore = {ArithmeticException.class})
    // 加入内部出现某种操作异常，就会调用fallback
    //流控的优先级高于异常处理
    public User getUser(String id){
        return new User("我的名字");
    }

    @PostConstruct
    public void initDegradeRule(){
        //降级规则
        List<DegradeRule> degradeRules=new ArrayList<>();
        DegradeRule degradeRule=new DegradeRule();
        degradeRule.setResource(DEGRADE_RESOURCE_NAME);
        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);

        //触发熔断的异常数
        degradeRule.setCount(2);
        //触发熔断的最小请求数
        degradeRule.setMinRequestAmount(2);
        //统计时长
        degradeRule.setStatIntervalMs(60*1000);//界面不存在，时默认一秒钟，单位ms
        //上述的时长就是用来统计请求，异常数量的时间时长

        //熔断持续时长
        degradeRule.setTimeWindow(10);
        //在熔断完成后，如果第一个请求又是异常的，系统则认为你没有完成相关措施的处理，就直接再次熔断

        degradeRules.add(degradeRule);
        DegradeRuleManager.loadRules(degradeRules);
    }

}
