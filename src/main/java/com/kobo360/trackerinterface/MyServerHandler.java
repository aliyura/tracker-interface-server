package com.kobo360.trackerinterface;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.Logger;
public class MyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = Logger.getLogger(MyServerHandler.class);

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端：" + ctx.channel().localAddress().toString() + " 通道开启！");
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("服务端：" + ctx.channel().localAddress().toString() + " 通道关闭！");
    }

    private String getMessage(ByteBuf buf) {
        byte[] con = new byte[buf.readableBytes()];
        buf.readBytes(con);
        try {
            return new String(con, "GBK");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) msg;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String mapKey = entry.getKey();
            String mapValue = entry.getValue().toString();
            System.out.println(mapKey + ":" + mapValue);
        }
        String jsonString = JSON.toJSONString(map);
        System.out.println("数据解析内容:" + jsonString);
        ReferenceCountUtil.release(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("...............数据接收-完毕...............");
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        String returnInfo = "服务端已接收数据！";
        ctx.writeAndFlush(Unpooled.copiedBuffer(returnInfo, CharsetUtil.UTF_8)).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("...............业务处理异常...............");
        cause.printStackTrace();
        ctx.close();
    }
}
