package robot;

import io.netty.channel.Channel;

public class RobotBase {
	private String account = null;//机器人账号
	private final Channel channel;
	
	public RobotBase(Channel channel){
		this.channel = channel;
	}
	
	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public Channel getChannel() {
		return channel;
	}
	
	
}
