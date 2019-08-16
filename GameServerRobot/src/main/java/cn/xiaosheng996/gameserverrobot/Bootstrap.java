package cn.xiaosheng996.gameserverrobot;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.xiaosheng996.gameserverrobot.robotManager.GameConfig;
import cn.xiaosheng996.gameserverrobot.robotManager.ProtoMgr;
import cn.xiaosheng996.gameserverrobot.robotManager.RobotMgr;

public class Bootstrap {

    private static final Log log = LogFactory.getLog(Bootstrap.class);

    public static void main(String[] args) throws Exception {
        try {
        	String configPath = args[0];
        	if (configPath.isEmpty()) {
        		log.error("机器人参数配置文件为空...");
				return;
			}
        	//加载外部机器人配置
        	RobotMgr.instance().loadRobotConfig(configPath);
    		//读取内部机器人配置文件
        	String serverConfig = GameConfig.getInstance().getServerConfigPath() + "/config/server.properties";
        	InputStream fis = new FileInputStream(new File(serverConfig));
        	Properties properties = new Properties();
            properties.load(fis);
    		GameConfig.getInstance().setProperties(properties);
    		//初始化协议
    		ProtoMgr.instance().initProtos();
        	//初始化网络
        	RobotMgr.instance().initNetty();
        	//启动机器人
        	RobotMgr.instance().start();
        } catch (Exception e) {
            log.error("机器人服务启动失败...", e);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
            	RobotMgr.instance().shutDownRobotMgr();
            } catch (Exception e) {
                log.error("机器人服务关闭失败...", e);
            }
        }));
    }

}
