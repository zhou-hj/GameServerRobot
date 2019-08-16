package cn.xiaosheng996.gameserverrobot.robotManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.xiaosheng996.gameserverrobot.constant.ConnectStatus;
import cn.xiaosheng996.gameserverrobot.constant.RobotType;
import cn.xiaosheng996.gameserverrobot.netty.Packet;
import cn.xiaosheng996.gameserverrobot.netty.ProtoDecoder;
import cn.xiaosheng996.gameserverrobot.netty.ProtoEncoder;
import cn.xiaosheng996.gameserverrobot.netty.WebSocketClientHandler;
import cn.xiaosheng996.gameserverrobot.robotType.RobotBase;
import cn.xiaosheng996.gameserverrobot.tools.CRC16CheckSum;
import cn.xiaosheng996.gameserverrobot.tools.ClassUtils;
import cn.xiaosheng996.gameserverrobot.tools.Probability;
import cn.xiaosheng996.gameserverrobot.tools.ProtoPrinter;

import com.google.protobuf.Message;

public class RobotMgr {
	private int robotType = 0;//机器人类型
	private int accountStart = 0;//机器人账号起始id
	private int robotNum = 0;//机器人数量
	private int serverId = 0;//服务器id
	public static String serverIp = null;//服务器ip
	public static int serverPort = 0;//服务器端口
	
	private static RobotMgr mgr = null;
	private static Logger log = LoggerFactory.getLogger(RobotMgr.class);
	
	protected static EventLoopGroup client;
    protected static Bootstrap bootstrap;
    
	//机器人集合channel -> robotBase
    private Map<Channel, RobotBase> robotsMap = new ConcurrentHashMap<>();
    private Vector<RobotBase> robotsList = new Vector<>();
	//任务调度
	private int threadNum; //实际开启机器人线程数量，因为有可能机器人数 < 配置的线程数
	private List<RobotThread> robotThreads = null; 
	
	public static RobotMgr instance(){
		if(mgr == null){
			mgr = new RobotMgr();
		}
		return mgr;
	}
	
	/**
	 * 加载机器人配置
	 * @param configPath
	 * @throws IOException
	 */
	public void loadRobotConfig(String configPath) throws IOException{
		FileInputStream fileInputStream = new FileInputStream(configPath);
		InputStreamReader streamReader = new InputStreamReader(fileInputStream, "GB2312");
		BufferedReader bufferedReader = new BufferedReader(streamReader);
		
		String line = null;
		String[] params = null;
		while ((line = bufferedReader.readLine()) != null) {
			String lineStr = line.trim();
			if (lineStr.isEmpty() || lineStr.startsWith("#")) {
				continue;
			}
			params = lineStr.split("=");
			if (params.length < 2)
				continue;
			if (lineStr.toUpperCase().startsWith("ROBOT_TYPE")){
				robotType = Integer.valueOf(params[1].trim());
			}else if (lineStr.toUpperCase().startsWith("ACCOUNT_START")) {
				accountStart = Integer.valueOf(params[1].trim());
			}else if (lineStr.toUpperCase().startsWith("ROBOT_NUM")) {
				robotNum = Integer.valueOf(params[1].trim());
			}else if (lineStr.toUpperCase().startsWith("SERVER_ID")) {
				serverId = Integer.valueOf(params[1].trim());
			}else if (lineStr.toUpperCase().startsWith("SERVER_IP")) {
				serverIp = params[1].trim();
			}else if (lineStr.toUpperCase().startsWith("SERVER_PORT")) {
				serverPort = Integer.valueOf(params[1].trim());
			}
		}
		
		fileInputStream.close();
		streamReader.close();
		bufferedReader.close();
	}
	
	/**
	 * 初始化网络
	 */
	public void initNetty(){
		client = new NioEventLoopGroup();
		bootstrap = new Bootstrap();
		bootstrap.group(client);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("http_codec", new HttpClientCodec());
				pipeline.addLast("http_aggregator", new HttpObjectAggregator(65536));
				pipeline.addLast("protobuf_decoder", new ProtoDecoder(null, 5120));
				pipeline.addLast("client_handler", new WebSocketClientHandler());
				pipeline.addLast("protobuf_encoder", new ProtoEncoder(new CRC16CheckSum(), 2048));
			}
		});
	}
	
	//机器人注册
	public void register(RobotBase robot){
		robotsList.add(robot);
	}

	/**
	 * 启动机器人
	 * @throws InterruptedException 
	 */
	public void start() throws InterruptedException{
		RobotType type = RobotType.valueOf(robotType);
		if(type == null){
			log.error("RobotType not exist, robotType:" + robotType);
			return;
		}

		for (int i = accountStart; i <= (accountStart + robotNum - 1); i++) {
			String account = new StringBuilder("robot").append(i).toString();
			type.registerRobot(account, ConnectStatus.STATUS_UNCONNECT);
		}
		
		//启动机器人任务调度
		int threads = GameConfig.getInstance().getRobotThreads();
		int openTime = GameConfig.getInstance().getOpenTime();
		initRobotThreads(threads, openTime);
	}

	//初始化机器人线程
	private void initRobotThreads(int threads, int openTime){
		int openBegin = (int)(System.currentTimeMillis()/1000);

		robotThreads = new ArrayList<>();
		threadNum = threads;
		if(robotsList.size() < threads){//如果机器人数  < 配置的线程数
			threadNum = robotsList.size();
		}

		//每个线程控制的机器人数量，如果15个机器人，有3个线程，则每个线程操控5个机器人，如果17个机器人，有3个线程，则每个线程操控6个机器人
		int ctrlRobotNum = robotsList.size() % threadNum == 0 ? robotsList.size() / threadNum : (int)Math.ceil((double)robotsList.size() / threadNum);
		for (int i = 0; i < threadNum ; i++) {
			RobotThread thread = new RobotThread(i, ctrlRobotNum);
			thread.setName(i+"号机器人管理线程");
			robotThreads.add(thread);
			thread.start();
		}
		
		int now = (int)(System.currentTimeMillis()/1000);
		if(openTime > 0 && now - openBegin >= openTime){
			shutDownRobotMgr();
		}
	}

	//到时关闭机器人
	public void shutDownRobotMgr(){
		robotThreads.forEach(e -> e.stopRun());
		robotThreads.clear();
		robotsList.clear();
		robotsMap.clear();
	}

	/**机器人管理线程*/
	private class RobotThread extends Thread{
		private int index;//机器人线程索引
		private int ctrlRobotNum;//当前线程操控的机器人数量
		private boolean isRun = true;//当前线程是否在运行状态

		public RobotThread(int index, int ctrlRobotNum){
			this.index = index;
			this.ctrlRobotNum = ctrlRobotNum;
		}

		@Override
		public void run() {
			int curCtrlIndex = 0; //当前线程当前操控的机器人索引
			while (isRun) {
				if(curCtrlIndex >= ctrlRobotNum){
					curCtrlIndex = 0;
				}
				if(index * ctrlRobotNum + curCtrlIndex >= robotsList.size()){
					curCtrlIndex = 0;
				}
				RobotBase robot = robotsList.get(index * ctrlRobotNum + curCtrlIndex);
				System.out.println(Thread.currentThread().getName()+"运行，当前运行的机器人是:"+robot.getAccount());
				robot.robotRun(System.currentTimeMillis());
				
				try {
					Thread.sleep(Probability.rand(200, 3000));//随机休眠一下，让每个机器人看起来不是一致的
				} catch (InterruptedException e) {
					e.printStackTrace();
				}finally{
					curCtrlIndex++;
				}
			}
		}

		//停止线程
		public void stopRun(){
			this.isRun = false;
		}
	}

	public void process(Channel channel, Packet packet){
		RobotBase robot = robotsMap.get(channel);
		if(robot == null){
			log.error("channel:" + channel.id() + "不存在该消息频道的机器人， robotsMap:" + robotsMap);
			return;
		}
		System.out.println("\n" + robot.getAccount() + "<<<<<<<<<<<<收到服务端协议:"+packet.getCmd()+"<<<<<<<<<<<<");

		//打印返回协议
		Class<?> clazz = ProtoMgr.instance().getMessageRespMap().get(packet.getCmd());
		if(clazz == null){
			System.out.println(packet.getCmd() + "返回协议不存在!");
			return;
		}
		Method m = ClassUtils.findMethod(clazz, "getDefaultInstance");
		Message msg = null;
		try {
			Message message = (Message) m.invoke(null);
			msg = message.newBuilderForType().mergeFrom(packet.getBytes()).build();
			System.out.println("返回协议 打印开始------------------------------------");
			ProtoPrinter.print(msg);
			System.out.println("返回协议 打印结束------------------------------------");
		} catch (Exception e) {
			log.error("处理返回协议:"+packet.getCmd()+"时报错", e);
		} 
		
		/**相应机器人模块处理协议*/
		robot.process(packet.getCmd(), msg);
	}


	public static Channel connect(){
		try {
			URI websocketURI = new URI(String.format("ws://%s:%d/", serverIp, serverPort));
			HttpHeaders httpHeaders = new DefaultHttpHeaders();

			//进行握手
			WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, (String)null, true,httpHeaders);
			Channel channel = bootstrap.connect(serverIp, serverPort).sync().channel();
			WebSocketClientHandler handler = (WebSocketClientHandler)channel.pipeline().get("client_handler");
			handler.setHandshaker(handshaker);
			// 通过它构造握手响应消息返回给客户端，
			// 同时将WebSocket相关的编码和解码类动态添加到ChannelPipeline中，用于WebSocket消息的编解码，
			// 添加WebSocketEncoder和WebSocketDecoder之后，服务端就可以自动对WebSocket消息进行编解码了
			handshaker.handshake(channel);
			//阻塞等待是否握手成功
			ChannelFuture future = handler.handshakeFuture().sync();
			if(future.isSuccess())
				return channel;
			return null;
		} catch (Exception e) {
			log.error("连接失败:", e);
			return null;
		}
	}

	public int getRobotType() {
		return robotType;
	}

	public void setRobotType(int robotType) {
		this.robotType = robotType;
	}

	public int getAccountStart() {
		return accountStart;
	}

	public void setAccountStart(int accountStart) {
		this.accountStart = accountStart;
	}

	public int getRobotNum() {
		return robotNum;
	}

	public void setRobotNum(int robotNum) {
		this.robotNum = robotNum;
	}

	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}

	public Map<Channel, RobotBase> getRobotsMap() {
		return robotsMap;
	}

	public void setRobotsMap(Map<Channel, RobotBase> robotsMap) {
		this.robotsMap = robotsMap;
	}

	public List<RobotThread> getRobotThreads() {
		return robotThreads;
	}

	public void setRobotThreads(List<RobotThread> robotThreads) {
		this.robotThreads = robotThreads;
	}
	
}
