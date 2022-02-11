package com.kobo360.trackerinterface;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.apache.log4j.Logger;

public class MyDecoder extends ByteToMessageDecoder {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Logger logger = Logger.getLogger(MyDecoder.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("...............数据解析异常 ...............");
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * ByteBuf转String * @param buf * @return * @throws UnsupportedEncodingException
     */
    public String convertByteBufToString(ByteBuf buf) throws UnsupportedEncodingException {
        String str;
        if (buf.hasArray()) {
            str = new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes());
        } else {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            str = new String(bytes, 0, buf.readableBytes());
        }
        return str;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {
        int dataLen = 0;
        int length=0;
        while (true) {
            //Look for the message header, discard all data if not found
            in.markReaderIndex();
            //get header
            int head=in.readShort();
            if(head==0x7878)
            {
                dataLen=in.readByte()&0xFF;
                length=1;
                //The readable length cannot be less than the data length plus the end of the packet
                if (in.readableBytes() < dataLen+2) {
                    in.resetReaderIndex();
                }
                break;
            }else if(head==0x7979)
            {
                dataLen=in.readShort();
                length=2;
                //The readable length cannot be less than the data length plus the end of the packet
                if (in.readableBytes() < dataLen+2) {
                    in.resetReaderIndex();
                }
                break;
            }
            //The readable length cannot be less than the base length
            if (in.readableBytes() < ConcoxConstant.MSG_BASE_LENGTH) {
                in.resetReaderIndex();
            }
        }
        ConcoxMessage concoxMessage = new ConcoxMessage();
        concoxMessage.setProtocolType(protocolType);
        //protocol number
        int msgId = in.readByte()&0xFF;
        concoxMessage.setMsgId(msgId);
        //Terminal number and message body processing
        if(msgId==0x01) {
            //read terminal number
            byte[] terminalNumArr = new byte[8];
            in.readBytes(terminalNumArr);
            String terminalNum = ByteBufUtil.hexDump(terminalNumArr);
            concoxMessage.setTerminalNum(terminalNum.replaceAll("^(0+)", ""));

            //Read the remaining message body (information content (remove device number) + information serial number)
            byte[] remainingMsgBodyArr = new byte[dataLen-11];
            in.readBytes(remainingMsgBodyArr);

            //Combined complete message body (packet length + protocol number + message content + message sequence number)
            ByteBuf frame= ByteBufAllocator.DEFAULT.heapBuffer(dataLen +1);
            //packet length
            frame.writeByte(dataLen);
            //protocol number
            frame.writeByte(msgId);
            //terminal number
            frame.writeBytes(terminalNumArr);
            // remaining message body
            frame.writeBytes(remainingMsgBodyArr);
            concoxMessage.setMsgBody(frame);
        }else{
            //read terminal number
            String terminalNum = SessionUtil.getTerminalInfo(ctx).getTerminalNum();
            concoxMessage.setTerminalNum(terminalNum);
            //Read the remaining message body (message content + serial number), remove the error check
            byte[] remainingMsgBodyArr = new byte[dataLen - 3];
            in.readBytes(remainingMsgBodyArr);
            //Combined complete message body (packet length + protocol number + message content + message sequence number)
            //Packet length byte + protocol number + message content + message sequence number (equivalent to message length - error check + packet length bytes)
            int msgLen = dataLen + length - 2;
            ByteBuf frame = ByteBufAllocator.DEFAULT.heapBuffer(msgLen);
            if (length == 1) {
                //packet length
                frame.writeByte(dataLen);
            } else {
                frame.writeShort(dataLen);
            }
            //protocol number
            frame.writeByte(msgId);
            //Remaining message body (message content + serial number)
            frame.writeBytes(remainingMsgBodyArr);
            concoxMessage.setMsgBodyArr(remainingMsgBodyArr);
            concoxMessage.setMsgBody(frame);
        }
        //read check
        in.readShort();
        //read end of packet
        in.readShort();
        // reclaim read bytes
        in.discardReadBytes();
        return concoxMessage;
    }
}