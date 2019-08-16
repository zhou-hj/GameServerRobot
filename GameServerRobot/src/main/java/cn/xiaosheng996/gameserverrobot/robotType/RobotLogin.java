package cn.xiaosheng996.gameserverrobot.robotType;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import proto.RoleProto.LoginReq_1001001;
import proto.RoleProto.LogoutReq_1001002;
import cn.xiaosheng996.gameserverrobot.constant.ConnectStatus;
import cn.xiaosheng996.gameserverrobot.constant.RobotType;
import cn.xiaosheng996.gameserverrobot.robotManager.RobotMgr;

import com.google.protobuf.Message;

/**
 * 登陆机器人
 */
public class RobotLogin extends RobotBase{
	private long lastLoginTime = 0;//上次登录时间
	private long lastOffLineTime = 0;//上次下线时间
	
	private static final int LOGINOUT_INTER_TIME_MILLIS = 5000;//登录下线间隔
	
	public RobotLogin(String account, int status) {
		super(account, status, RobotType.ROBOT_TYPE_LOGIN);
	}
	
	@Override
	public void robotRun(long time) {
		if(getStatus() == ConnectStatus.STATUS_CONNECTING){
			login();
		}else if (getStatus() == ConnectStatus.STATUS_CONNECTED) {
			if(time -lastLoginTime > LOGINOUT_INTER_TIME_MILLIS){
				logout();
			}
		}else if (getStatus() == ConnectStatus.STATUS_UNCONNECT) {
			if(time - lastOffLineTime > LOGINOUT_INTER_TIME_MILLIS){
				connect();
			}
		}
	}
	
	@Override
	public void process(int respCmd, Message message) {
		if (respCmd == 1001001){
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
	public void login(){
		LoginReq_1001001.Builder builder = LoginReq_1001001.newBuilder();
		builder.setAccount(String.valueOf(getChannel().hashCode()));
		builder.setPassword("jianshu");
		send(builder.build());
	}
	
	/**请求退出*/
	public void logout(){
		LogoutReq_1001002.Builder builder = LogoutReq_1001002.newBuilder();
		builder.setRid(Long.valueOf(getChannel().hashCode()));
		send(builder.build());
	}
	
	/**连接服务器*/
	public void connect(){
		Channel channel = RobotMgr.connect();
		if (channel != null){
			RobotMgr.instance().getRobotsMap().put(channel, this);
			setChannel(channel);
			setStatus(ConnectStatus.STATUS_CONNECTING);
		}
	}
}
