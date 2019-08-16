package cn.xiaosheng996.gameserverrobot.robotManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.xiaosheng996.gameserverrobot.tools.ClassUtils;

import com.google.protobuf.Message;

public class ProtoMgr {
	//机器人随机请求的协议，此容器里的协议才会随机发往服务器
	private Map<Integer, Class<?>> messageReqMap = new HashMap<>();
	//机器人过滤返回的协议，不在此容器的协议将被过滤
    private Map<Integer, Class<?>> messageRespMap = new HashMap<>();
	
	private static Log log = LogFactory.getLog(ProtoMgr.class);
	
	private static ProtoMgr instance = null;
	public static ProtoMgr instance(){
		if(instance == null){
			instance = new ProtoMgr();
		}
		return instance;
	}
	
	//初始化协议
	@SuppressWarnings("rawtypes")
	public void initProtos(){
		String reqModule = GameConfig.getInstance().getReqModule();
		String respModule = GameConfig.getInstance().getRespModule();
		List<Integer> reqList = GameConfig.getInstance().getReqList();
		List<Integer> respList = GameConfig.getInstance().getRespList();
		
		String packageName = "proto";
        Class clazz = Message.class;
        try {
        	TreeMap<Integer, Class<?>> reqMap = ClassUtils.getClasses(packageName, clazz, "Req_");
        	TreeMap<Integer, Class<?>> respMap = ClassUtils.getClasses(packageName, clazz, "Resp_");
        	
        	//解析配置文件中请求的协议
        	if(reqModule.equals("Req_")){
        		messageReqMap.putAll(reqMap);
        	}else {
        		TreeMap<Integer, Class<?>> _reqMap = ClassUtils.getClasses(packageName, clazz, reqModule);
        		messageReqMap.putAll(_reqMap);
        		if(reqList.size() > 0){
        			reqList.forEach(e -> {
        				Class<?> c = reqMap.get(e);
        				if(c != null && !messageReqMap.containsKey(e)){
        					messageReqMap.put(e, c);
        				}
        			});
        		}
			}
        	
        	//解析配置文件中过滤返回的协议
        	if(respModule.equals("Resp_")){
        		messageRespMap.putAll(respMap);
        	}else if(respList.size() > 0 && !respList.contains(0)){
        		respList.forEach(e -> {
    				Class<?> c = respMap.get(e);
    				if(c != null && !messageRespMap.containsKey(e)){
    					messageRespMap.put(e, c);
    				}
    			});
			}else {
				TreeMap<Integer, Class<?>> _respMap = ClassUtils.getClasses(packageName, clazz, respModule);
				messageRespMap.putAll(_respMap);
			}
        	
        	System.out.println("机器人管理器随机请求的协议列表为:");
        	messageReqMap.forEach((k, c) -> {
        		System.out.println("ReqMap cmd:" + k + ",simple class name:" + c.getSimpleName());
        	});
        	System.out.println("\n机器人管理器筛选返回的协议列表为:");
        	messageRespMap.forEach((k, c) -> {
        		System.out.println("RespMap cmd:" + k + ",simple class name:" + c.getSimpleName());
        	});
        } catch (Throwable e) {
            e.printStackTrace();
        }
	}
	
	
	public Map<Integer, Class<?>> getMessageReqMap() {
		return messageReqMap;
	}

	public Map<Integer, Class<?>> getMessageRespMap() {
		return messageRespMap;
	}
}
