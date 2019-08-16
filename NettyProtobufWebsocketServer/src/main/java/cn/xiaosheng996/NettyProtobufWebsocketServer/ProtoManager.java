package cn.xiaosheng996.NettyProtobufWebsocketServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import proto.RoleProto.LoginReq_1001001;
import proto.RoleProto.LoginResp_1001001;
import proto.RoleProto.LogoutResp_1001002;
import robot.RobotBase;

import com.google.protobuf.Message;

@SuppressWarnings("rawtypes")
public class ProtoManager {
	
	private static Map<Integer, Class<?>> reqMap = null;
	private static Map<Integer, Class<?>> respMap = null;
	
	private static Logger logger = LoggerFactory.getLogger(ProtoManager.class);

    static {
        String packageName = "proto";
        Class clazz = Message.class;
        try {
            reqMap = ClassUtils.getClasses(packageName, clazz, "Req_");
            respMap = ClassUtils.getClasses(packageName, clazz, "Resp_");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("服务端支持的请求的协议列表为:");
        reqMap.forEach((k, c) -> {
    		System.out.println("ReqMap cmd:" + k + ",simple class name:" + c.getSimpleName());
    	});
    	System.out.println("\n服务端支持的返回的协议列表为:");
    	respMap.forEach((k, c) -> {
    		System.out.println("RespMap cmd:" + k + ",simple class name:" + c.getSimpleName());
    	});
    }

    public static ByteBuf wrapBuffer(Message msg) {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;
        int protocol = 0;
        Set<Entry<Integer, Class<?>>> set = respMap.entrySet();
        for (Entry<Integer, Class<?>> entry : set) {
            if (entry.getValue().isInstance(msg)) {
                protocol = entry.getKey();
                break;
            }
        }
        byte[] data = msg.toByteArray();
        // 消息长度=协议号4位+数据体长度
        int length = data.length + 4;
        // 数据包=消息长度+协议号+数据体
        // 数据包长度=4+消息长度
        // ByteBuf buffer = Unpooled.buffer(length + 4);
        ByteBuf buffer = alloc.buffer(length + 4);
        buffer.writeByte((byte)0x80);
        buffer.writeShort(length);
        buffer.writeInt(protocol);
        buffer.writeBytes(data);

        if (buffer.readableBytes() > 4096) {
//            LogUtil.warn(ProtobufCenter.toString(protocol) + " " + buffer.readableBytes() + " too big");
        }
        return buffer;
    }
    
    public static Map<Integer, Class<?>> getReqMap() {
    	return reqMap;
    }
    
    public static void handleProto(Packet packet, RobotBase robot){
    	//游戏业务线程池处理游戏逻辑
    	//......
    	//......
    	
    	int reqCmd = packet.getCmd();
    	if(reqCmd == 1001001){
    		LoginResp(robot.getChannel());
    	}else if(reqCmd == 1001002){
    		LogoutResp(robot.getChannel());
    	}
    	
    	//打印协议
    	Class<?> clz = getReqMap().get(packet.getCmd());
		try {
			Method method = clz.getMethod("parseFrom", byte[].class);
			Object object = method.invoke(clz, packet.getBytes());
			if(reqCmd == 1001001){
				LoginReq_1001001 builder = (LoginReq_1001001)object;
				robot.setAccount(builder.getAccount());
			}
			
			System.out.println("收到"+ robot.getAccount()+"请求的协议:"+packet.getCmd());
			ProtoPrinter.print(object);
			System.out.println();
		} catch (Exception e1) {
			logger.error("cmd:"+packet.getCmd()+", error:", e1);
		}
    }
    
    public static int getMessageID(Message msg) {
		int protocol = 0;
        Set<Entry<Integer, Class<?>>> set = respMap.entrySet();
        for (Entry<Integer, Class<?>> entry : set) {
            if (entry.getValue().isInstance(msg)) {
                protocol = entry.getKey();
                break;
            }
        }
        return protocol;
	}
    
    //发送协议
	public static void send(Message msg, Channel channel) {
		if (channel == null || msg == null || !channel.isWritable()) {
			return;
		}
		int cmd = ProtoManager.getMessageID(msg);
		Packet packet = new Packet(Packet.HEAD_TCP, cmd, msg.toByteArray());
		channel.writeAndFlush(packet);
	}
	
	private static void LoginResp(Channel channel) {
		LoginResp_1001001.Builder builder = LoginResp_1001001.newBuilder();
		builder.setAccount(String.valueOf(channel.hashCode()));
		builder.setRid(Long.valueOf(channel.hashCode()));
		builder.setLevel((int)(Math.random() * 100));
		send(builder.build(), channel);
	}
	
	private static void LogoutResp(Channel channel){
		LogoutResp_1001002.Builder builder = LogoutResp_1001002.newBuilder();
		builder.setRid(Long.valueOf(channel.hashCode()));
		send(builder.build(), channel);
	}
}
