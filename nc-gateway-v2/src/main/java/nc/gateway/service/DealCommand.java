package nc.gateway.service;

import com.alibaba.fastjson.JSONObject;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import lombok.extern.slf4j.Slf4j;
import nc.gateway.annotation.scan.AnnotationScan;
import nc.gateway.util.Constant;
import nc.gateway.util.E2s;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DealCommand {

    @Autowired
    AnnotationScan scan;

    @Autowired
    CommonFunction commonFunction;

    private static Map<String, Object> serverCache = new HashMap<>();
    private static Map<String, Method> methodCache = new HashMap<>();

    @HystrixCommand(fallbackMethod = "commandFallBack",
            commandProperties = {
                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "50"),
                    @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "1440"),
                    @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "20")
            })
    public String executeCommand(String command, HttpServletRequest request) {
        String logPre = "command=" + command;

        //网关返回的Json数据
        JSONObject retJson = new JSONObject();
        String retStr = null;

        //获取请求参数
        Map<String, String> paramMap = new HashMap<String, String>();
        Enumeration<String> parameters = request.getParameterNames();
        while (parameters.hasMoreElements()){
            String tmpKey = parameters.nextElement();
            paramMap.put(tmpKey, String.valueOf(request.getParameter(tmpKey)));
        }

        if (!commonFunction.checkParam(paramMap)) {
            log.error(logPre + "err=parameters_error");
            retJson.put(Constant.JSON_RESP_CODE, Constant.RESP_CODE_LEAK_PARAM);
            retJson.put(Constant.JSON_ERROR_MSG, "请求参数非法");
            retStr = retJson.toString();
            return retStr;
        }

        JSONObject confJson = (JSONObject) scan.getCmdMap().get(command);
        //判断请求是否注册
        if (null == confJson || 0 == confJson.size()) {
            log.error(logPre + "no_command_found");
            retJson.put(Constant.JSON_RESP_CODE, Constant.RESP_CODE_LEAK_PARAM);
            retJson.put(Constant.JSON_ERROR_MSG, "请求参数非法");
            retStr = retJson.toString();
            return retStr;
        }

        String serverClass = confJson.get(AnnotationScan.DEFAULT_RPCCLASS_NAME).toString();

        Object server = getRpcServer(confJson);  //反射缓存
        Method method = null;
        method = getRpcMethod(confJson, serverClass); //反射缓存
        if (null == method) {
            log.error(logPre + "err=dubbo_reflect_error");
            retJson.put(Constant.JSON_RESP_CODE, Constant.RESP_CODE_LEAK_PARAM);
            retJson.put(Constant.JSON_ERROR_MSG, "服务端错误，请稍后重试");
            retStr = retJson.toString();
            return retStr;
        }

        try {
            retStr = (String) method.invoke(server, paramMap);
        } catch (IllegalAccessException e) {
            log.error(logPre + E2s.exception2String(e));
        } catch (InvocationTargetException e) {
            log.error(logPre + E2s.exception2String(e));
        }

        if (null == retStr) {
            retJson.put(Constant.JSON_RESP_CODE, Constant.RESP_CODE_SERVER_ERROR);
            retJson.put(Constant.JSON_ERROR_MSG, "服务端错误，请稍后重试");
            retStr = retJson.toString();
            return retStr;
        }

        return retStr;
    }

    public String commandFallBack(String command, HttpServletRequest request) {
        //网关返回的Json数据
        JSONObject retJson = new JSONObject();
        String retStr = null;
        retJson.put(Constant.JSON_RESP_CODE, Constant.RESP_CODE_SERVER_ERROR);
        retJson.put(Constant.JSON_ERROR_MSG, "Hystrix fallback: 服务端错误，请稍后重试");
        retStr = retJson.toString();
        return retStr;
    }

    private static Object getRpcServer(JSONObject confJson) {
        String serviceUrl = confJson.get(AnnotationScan.DEFAULT_SERVER_REG_NAME).toString();
        if (null == serviceUrl) {
            log.error("desc=scfurl_error name=" + confJson.get("name").toString());
            return null;
        }
        String scfClass = confJson.get(AnnotationScan.DEFAULT_RPCCLASS_NAME).toString();
        if (null == scfClass) {
            log.error("desc=scfclass_error name=" + confJson.get("name").toString());
            return null;
        }

        String mapKey = new StringBuilder(serviceUrl).append("&").append(scfClass).toString();

        Object scfObject = serverCache.get(mapKey);
        if (null == scfObject) {
            try {
                scfObject = Proxy.newProxyInstance(DealCommand.class.getClassLoader(), new Class[]{Class.forName(scfClass)}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        //TODO: Call RPC method
                        return "Request completed";
                    }
                });  //获取代理对象
                serverCache.put(mapKey, scfObject); //避免重复初始化
            } catch (ClassNotFoundException e) {
                // TODO: handle exception
                log.error("desc=reflect_class_not_found class=" + scfClass);
                return null;
            }
        }

        return scfObject;
    }

    private static Method getRpcMethod(JSONObject confJson, String scfClass) {

        String scfMethod = confJson.get(AnnotationScan.DEFAULT_RPCMETHOD_NAME).toString();
        if (null == scfMethod) {
            log.error("desc=scfmethod_error name=" + confJson.get("name").toString());
            return null;
        }
        String mapMethodKey = new StringBuilder().append(scfClass)
                .append("&").append(scfMethod).toString();
        Method method = methodCache.get(mapMethodKey);    //scf方法
        if (null != method) {
            return method;
        }
        //新建method
        Class<?>[] paramTypes = new Class<?>[]{Map.class};
        if (1 != paramTypes.length) {
            //确保接入层到下游的接口是统一的
            log.error("desc=param_len_err");
            return null;
        }
        try {
            method = Class.forName(scfClass).getMethod(scfMethod, paramTypes);
            methodCache.put(mapMethodKey, method);

        } catch (Exception e) {
            log.error("desc=get_scfmethod class=" + scfClass + " method=" + scfMethod,
                    " e=" + E2s.exception2String(e));
        }
        return method;
    }
}
