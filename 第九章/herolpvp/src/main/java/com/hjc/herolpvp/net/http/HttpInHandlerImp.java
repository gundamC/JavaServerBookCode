package com.hjc.herolpvp.net.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hjc.herolpvp.core.GameServer;
import com.hjc.herolpvp.task.ExecutorPool;
import com.hjc.herolpvp.util.Constants;
import com.hjc.herolpvp.util.encrypt.XXTeaCoder;

public class HttpInHandlerImp {
	private static Logger log = LoggerFactory.getLogger(HttpInHandlerImp.class);
	public static String DATA = "data";
	public static volatile boolean CODE_DEBUG = false;

	public void channelRead(final ChannelHandlerContext ctx, final Object msg)
			throws Exception {
		/** work线程的内容转交线程池管理类处理，缩短work线程耗时 **/
		ExecutorPool.channelHandleThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				if (!GameServer.shutdown) {// 服务器开启的情况下
					DefaultFullHttpRequest req = (DefaultFullHttpRequest) msg;
					if (req.getMethod() == HttpMethod.GET) { // 处理get请求
						getHandle(ctx, req);
					}
					if (req.getMethod() == HttpMethod.POST) { // 处理POST请求
						postHandle(ctx, req);
					}
				} else {// 服务器已关闭
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("errMsg", "server closed");
					writeJSON(ctx, jsonObject);
				}
			}
		});
	}

	private void postHandle(final ChannelHandlerContext ctx,
			final DefaultFullHttpRequest req) {
	}

	private void getHandle(final ChannelHandlerContext ctx,
			DefaultFullHttpRequest req) {
	}

	/**
	 * @Title: codeFilter
	 * @Description: 编解码过滤
	 * @param val
	 * @return
	 * @throws UnsupportedEncodingException
	 *             String
	 * @throws
	 */
	private String codeFilter(String val) throws UnsupportedEncodingException {
		val = val.contains("%") ? URLDecoder.decode(val, "UTF-8") : val;
		String valTmp = val;
		val = CODE_DEBUG ? XXTeaCoder.decryptBase64StringToString(val,
				XXTeaCoder.key) : val;
		if (Constants.MSG_LOG_DEBUG) {
			if (val == null) {
				val = valTmp;
			}
		}
		return val;
	}

	public static void writeJSON(ChannelHandlerContext ctx,
			HttpResponseStatus status, Object msg) {
		String sentMsg = null;
		if (msg instanceof String) {
			sentMsg = (String) msg;
		} else {
			sentMsg = JSON.toJSONString(msg);
		}
		sentMsg = CODE_DEBUG ? XXTeaCoder.encryptToBase64String(sentMsg,
				XXTeaCoder.key) : sentMsg;
		writeJSON(ctx, status,
				Unpooled.copiedBuffer(sentMsg, CharsetUtil.UTF_8));
		ctx.flush();
	}

	public static void writeJSON(ChannelHandlerContext ctx, Object msg) {
		String sentMsg = null;
		if (msg instanceof String) {
			sentMsg = (String) msg;
		} else {
			sentMsg = JSON.toJSONString(msg);
		}
		sentMsg = CODE_DEBUG ? XXTeaCoder.encryptToBase64String(sentMsg,
				XXTeaCoder.key) : sentMsg;
		writeJSON(ctx, HttpResponseStatus.OK,
				Unpooled.copiedBuffer(sentMsg, CharsetUtil.UTF_8));
		ctx.flush();
	}

	private static void writeJSON(ChannelHandlerContext ctx,
			HttpResponseStatus status, ByteBuf content /*
														 * , boolean isKeepAlive
														 */) {
		if (ctx.channel().isWritable()) {
			FullHttpResponse msg = null;
			if (content != null) {
				msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
						content);
				msg.headers().set(HttpHeaders.Names.CONTENT_TYPE,
						"application/json; charset=utf-8");
				msg.headers().set("userid", 101);
			} else {
				msg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
			}
			if (msg.content() != null) {
				msg.headers().set(HttpHeaders.Names.CONTENT_LENGTH,
						msg.content().readableBytes());
			}
			// not keep-alive
			ctx.write(msg).addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		log.error("netty exception:", cause);
	}
}
