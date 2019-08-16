package cn.xiaosheng996.gameserverrobot.robotType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import proto.RoleProto.LoginReq_1001001;
import proto.RoleProto.LoginResp_1001001;
import cn.xiaosheng996.gameserverrobot.constant.ConnectStatus;
import cn.xiaosheng996.gameserverrobot.constant.RobotType;
import cn.xiaosheng996.gameserverrobot.robotManager.ProtoMgr;
import cn.xiaosheng996.gameserverrobot.robotManager.RobotMgr;
import cn.xiaosheng996.gameserverrobot.tools.Probability;
import cn.xiaosheng996.gameserverrobot.tools.ProtoPrinter;

import com.google.protobuf.LazyStringList;
import com.google.protobuf.Message;

/**
 * 协议机器人
 *
 */
public class RobotProto extends RobotBase {

	public RobotProto(String account, int status) {
		super(account, status, RobotType.ROBOT_TYPE_PROTO);
	}

	@Override
	public void robotRun(long time) {
		//必须先登录才能发送协议，因为有些功能玩法必须登录游戏
		if (getStatus() == ConnectStatus.STATUS_UNCONNECT){
			connect();
		}else if(getStatus() == ConnectStatus.STATUS_CONNECTING){
			login();
		}else if(getStatus() == ConnectStatus.STATUS_CONNECTED){
			randProto();
		}
		
	}

	@Override
	public void process(int respCmd, Message message) {
		if (respCmd == 1001001){
			LoginResp_1001001 builder = (LoginResp_1001001)message;
			setRobotRid(builder.getRid());
			setStatus(ConnectStatus.STATUS_CONNECTED);
		}else if(respCmd == 1001002){
			setStatus(ConnectStatus.STATUS_UNCONNECT);
			try {
				Channel channel = getChannel();
				ChannelFuture future = channel.close().sync();
				if (future.isSuccess()){
					System.out.println(channel.hashCode() + "退出登录成功.......");
				}
			} catch (InterruptedException e) {
				log.error("退出失败：", e);
			}
		}
	}
	
	/**请求登录*/
	private void login(){
		LoginReq_1001001.Builder builder = LoginReq_1001001.newBuilder();
		builder.setAccount(getAccount());
		builder.setPassword("jianshu");
		send(builder.build());
	}

	/**连接服务器*/
	private void connect(){
		Channel channel = RobotMgr.connect();
		if (channel != null){
			RobotMgr.instance().getRobotsMap().put(channel, this);
			setChannel(channel);
			setStatus(ConnectStatus.STATUS_CONNECTING);
		}
	}
	
	/**随机发送协议*/
	private void randProto(){
		Map<Integer, Class<?>> reqMap = ProtoMgr.instance().getMessageReqMap();
		List<Integer> cmds = new ArrayList<>(reqMap.keySet());
		int cmd = cmds.get(Probability.rand(0, cmds.size()-1));
		Class<?> clazz = reqMap.get(cmd);
		
		sendReq(cmd, clazz);
	}
	
	private void sendReq(int cmd, Class<?> clazz){
		try {
			Class<?> buildClass = null;
	        for (Class<?> cls : clazz.getDeclaredClasses()) {
	            if ("Builder".equals(cls.getSimpleName())) {
	                buildClass = cls;
	                break;
	            }
	        }
	        
	        Method newMethod = clazz.getDeclaredMethod("newBuilder", new Class[0]);
            Object builder = newMethod.invoke(clazz, new Object[0]);
            Method buildMethod = builder.getClass().getDeclaredMethod("build", new Class[0]);
	        if (buildClass != null) {
	            for (Field field : buildClass.getDeclaredFields()) {
	                if ("bitField0_".equals(field.getName())) {
	                    continue;
	                }
	                
	                String fieldName = field.getName().replaceAll("_", "");
	                Method setter = null;
	                Class<?> parameterClass = null;
	                if (field.getType().isAssignableFrom(String.class)) {
	                    parameterClass = String.class;
	                    setter = builder.getClass().getDeclaredMethod(
	                        "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), 
	                        parameterClass);
	                } else if (field.getType().isAssignableFrom(List.class)) {
	                	parameterClass = (Class<?>) ((ParameterizedType) field.getGenericType())
	            				.getActualTypeArguments()[0];
	            		setter = builder.getClass().getDeclaredMethod(
	            				"add" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1), 
	            				getMethodClass(parameterClass));
	                } else if (field.getType().isAssignableFrom(LazyStringList.class)){
	                	setter = builder.getClass().getDeclaredMethod(
	                			"add" + fieldName.substring(0, 1).toUpperCase()+ fieldName.substring(1), 
	                			getMethodClass(String.class));
	                } else {
	                	parameterClass = field.getType();
	                    setter = builder.getClass().getDeclaredMethod(
	                        "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1), 
	                        parameterClass);
	                }
	                if (field.getType().isAssignableFrom(String.class)) {
	                    setter.invoke(builder, Probability.randomString(Probability.rand(3, 8)));
	                } else if ("int".equals(field.getType().getSimpleName())) {
	                    setter.invoke(builder, Probability.rand(1, 10));
	                } else if ("long".equals(field.getType().getSimpleName())) {
	                    setter.invoke(builder, Probability.rand(100000000, 1000000000));
	                } else if (field.getType().isAssignableFrom(List.class)
	                		|| field.getType().isAssignableFrom(LazyStringList.class)) {
	                    int num = Probability.rand(2, 4);
	                    for (int i = 0; i < num; i++) {
	                        if (parameterClass == Long.class) {
	                            setter.invoke(builder, Probability.rand(100000000, 1000000000));
	                        } else if (parameterClass == Integer.class) {
	                            setter.invoke(builder, Probability.rand(1, 10));
	                        } else {
	                            setter.invoke(builder, Probability.randomString(Probability.rand(3, 8)));
	                        }
	                    }
	                }
	            }
	        }
            
            //print("\n 协议:"+protocol+"参数如下:");
            Object builded = buildMethod.invoke(builder, new Object[0]);
            System.out.println("请求协议 参数如下:------------------------------------");
            ProtoPrinter.print(builded);
            System.out.println();
            send((Message) builded);
        }catch(Exception e){
        	e.printStackTrace();
        }
	}
	
    private Class<?> getMethodClass(Class<?> clz) {
    	if (clz == Integer.class) {
    		return int.class;
    	} else if (clz == Long.class) {
    		return long.class;
    	} else {
    		return clz;
    	}
    }
}
