package nettytest;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pvpclient.Pvp;

import com.alibaba.fastjson.JSONObject;
import com.hjc.herolpvp.net.ProtoIds;

public class SocketInHandler extends ChannelHandlerAdapter {
	public static ChannelHandlerContext ctx = null;

	private static final Logger logger = LoggerFactory
			.getLogger(SocketInHandler.class);

	public static void write(ChannelHandlerContext ctx, String msg) {
		// byte[] req = msg.getBytes();
		// ByteBuf buf = Unpooled.buffer(req.length);
		// buf.writeBytes(req);
		// ctx.writeAndFlush(buf);
		ctx.writeAndFlush(msg);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		SocketInHandler.ctx = ctx;
		JSONObject req = new JSONObject();
		req.put("typeid", ProtoIds.TEST);
		SocketInHandler.write(SocketInHandler.ctx, req.toJSONString());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		// ByteBuf buf = (ByteBuf) msg;
		// byte[] req = new byte[buf.readableBytes()];
		// buf.readBytes(req);
		// String body = new String(req, "UTF-8");
		String body = (String) msg;
		if (body.equals("\"delay\"")) {// 测试网络延迟
			long delay = System.currentTimeMillis() - Pvp.netDelayStart;
			Pvp.netDelayLabel.setText(delay + "ms");
		} else {
			System.out.println("client read: " + body);
			Pvp.consoleArea.append(body + "\r\n");
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// 释放资源
		logger.warn("Unexpected exception : "
				+ cause.getMessage());
		ctx.close();
	}
}
