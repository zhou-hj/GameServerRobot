package cn.xiaosheng996.gameserverrobot.robotManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameConfig {
	private static Logger logger = LoggerFactory.getLogger(GameConfig.class);
	private static GameConfig instance = new GameConfig();
	public final static String SERVER_CONFIG = "server.dir";
	
	//指定的协议号列表
	private List<Integer> reqList = new ArrayList<>();
	//指定的返回协议号列表
	private List<Integer> respList = new ArrayList<>();
	
	/**
	 * 系统配置属性
	 */
	public Properties properties = new Properties();

	private GameConfig() {
	}

	public static GameConfig getInstance() {
		return instance;
	}
	
	public String getServerConfigPath(){
		return System.getProperty(SERVER_CONFIG);
	}

	public String getProperty(String key) {
		return (String) properties.get(key);
	}

	public void setProperty(String key, Object value) {
		_setProperty(key, value);
	}

	private <K, V> void _setProperty(K key, V value) {
		try {
			if (properties.containsKey(key)) {
				throw new RuntimeException("配置参数key重复:" + key + "," + value);
			}
			properties.put(key, value);
		} catch (Exception e) {
			logger.error("setProperty [" + key + "]" + " = [" + value + "]", e);
		}
	}

	public void setProperties(Properties properties) {
		properties.forEach((k, v) -> {
			_setProperty(k, v);
		});
	}
	
	//获取开启机器人线程数
	public int getRobotThreads(){
		return Integer.valueOf(getProperty("robot_threads"));
	}
	
	//获取机器人管理器开启时长
	public int getOpenTime(){
		return Integer.valueOf(getProperty("open_time"));
	}
	
	//指定模块请求协议
	public String getReqModule(){
		return getProperty("req_module");
	}
	
	//指定模块返回协议过滤
	public String getRespModule(){
		return getProperty("resp_module");
	}
	
	//获取指定的协议号列表
	public List<Integer> getReqList(){
		if(reqList.size() > 0)
			return reqList;
		String reqStr = getProperty("req_list");
		String[] reqs = reqStr.split(",");
		for (String req : reqs) {
			reqList.add(Integer.valueOf(req));
		}
		return reqList;
	}
	
	//显示指定的返回协议号
	public List<Integer> getRespList(){
		if(respList.size() > 0)
			return respList;
		String reqStr = getProperty("resp_list");
		String[] reqs = reqStr.split(",");
		for (String req : reqs) {
			respList.add(Integer.valueOf(req));
		}
		return respList;
	}
}
