package cn.xiaosheng996.NettyProtobufWebsocketServer;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import robot.RobotBase;


@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter{

    private static final Logger log = LoggerFactory.getLogger(ServerHandler.class);


    private final ConcurrentMap<Channel, RobotBase> ref = new ConcurrentHashMap<>();

    protected ServerHandler() {
    	
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        
        log.info("[{}] connected", ctx.channel().remoteAddress());
        System.out.println("channel:"+ctx.channel().hashCode()+"请求连接");
        ref.putIfAbsent(ctx.channel(), new RobotBase(ctx.channel()));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Packet packet = (Packet)msg;
        RobotBase robot = ref.get(ctx.channel());
        
        ProtoManager.handleProto(packet, robot);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        
        System.out.println("----channel:"+ctx.channel().hashCode()+"断开连接");
        RobotBase robot = ref.remove(ctx.channel());
        if (robot != null)
        	robot.getChannel().close();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        if (ctx.channel().isWritable()) {
        	RobotBase robot = ref.get(ctx.channel());
            if (log.isDebugEnabled())
                log.debug("connection["+robot.getAccount()+"] is available, flush the queue of connection");
            if (robot != null)
            	robot.getChannel().flush();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("handle packet from ["+ctx.channel().id().asLongText()+"] failed!", cause);
        RobotBase robot = ref.get(ctx.channel());
        if (robot != null) {
        	robot.getChannel().close();
        } else {
            log.error("connection["+ctx.channel().id().asLongText()+"] not found in manager");
            ctx.channel().close();
        }
    }
}
