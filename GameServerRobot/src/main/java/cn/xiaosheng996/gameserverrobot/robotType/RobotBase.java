package cn.xiaosheng996.gameserverrobot.robotType;


import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.xiaosheng996.gameserverrobot.constant.RobotType;
import cn.xiaosheng996.gameserverrobot.netty.Packet;
import cn.xiaosheng996.gameserverrobot.tools.ClassUtils;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;


public abstract class RobotBase{
	private String account = null;//机器人账号
	private RobotType robotType = null;//机器人类型
	private int status = 0;//机器人状态
	
	private long robotRid = 0;//机器人rid
	private Channel channel = null;//机器人通信channel
	
	public static Log log = LogFactory.getLog(RobotBase.class);
	
	//机器人各自行为函数
	abstract public void robotRun(long time);
	//机器人协议处理函数
	abstract public void process(int cmd, Message message);
	
	public RobotBase(String account, int status, RobotType robotType){
		this.account = account;
		this.status = status;
		this.robotType = robotType;
	}
	
	//发送消息给服务器
	public void send(Message message) {
        int cmd = ClassUtils.getMessageID(message.getClass());
        Packet packet = new Packet(Packet.HEAD_TCP, cmd, message.toByteArray());
        channel.writeAndFlush(packet).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if(channelFuture.isSuccess()){
					System.out.println("\n"+ getAccount() + ">>>>>>>>>>>发送协议:"+ cmd + "成功>>>>>>>>>>>");
				}else{
					System.out.println("\n"+ getAccount() + ">>>>>>>>>>>发送协议:"+ cmd + "失败xxxxxxxxxxx");
				}
			}
		});
        if (log.isDebugEnabled())
        	log.debug("send to " + robotRid + ":" + message.getClass().getSimpleName() + "," + TextFormat.printToUnicodeString(message));
    }
	
	
	private boolean isActive(){
		return channel.isActive();
	}
	
	private boolean isWritable(){
		return channel.isWritable();
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public RobotType getRobotType() {
		return robotType;
	}

	public void setRobotType(RobotType robotType) {
		this.robotType = robotType;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public long getRobotRid() {
		return robotRid;
	}

	public void setRobotRid(long robotRid) {
		this.robotRid = robotRid;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
}
