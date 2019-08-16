package cn.xiaosheng996.gameserverrobot.netty;


public class Packet {
	public static final byte HEAD_TCP = (byte)0x80;

	public static final byte HEAD_UDP = (byte)0;

	public static final byte HEAD_NEED_ACK = (byte)0x40;

	public static final byte HEAD_ACK = (byte)0x2c;

	public static final byte HEAD_PROTOCOL_MASK = (byte)0x03;

	public static final byte PROTOCOL_PROTOBUF = 0;

	public static final byte PROTOCOL_JSON = 1;
	
	/*
	 * 包头：
	 *  0   0   0   0   0   0   0   0
	 *  -   -   -   -   -   -   -   -
	 *  |   |   |   |   |   |   |   |
	 *  |   |   |   |   |   |   |   +-----协议类型
	 *  |   |   |   |   |   |   +---------协议类型
	 *  |   |   |   |   |   +-------------保留
	 *  |   |   |   |   +-----------------保留
	 *  |   |   |   +---------------------保留
	 *  |   |   +-------------------------回应
	 *  |   +-----------------------------是否需要确认
	 *  +---------------------------------TCP1,UDP0
	 */
    private final byte head;

    private final short sid;

    private final int cmd;

    private final byte[] bytes;

    
    public Packet(byte head, int cmd, byte[] bytes) {
    	this(head, (short)0, cmd, bytes);
    }

    public Packet(byte head, short sid, int cmd, byte[] bytes) {
	    this.cmd = cmd;
	    this.bytes = bytes;
	    this.head = head;
	    this.sid = sid;
    }

	public byte getHead() {
		return head;
	}


	public int getCmd() {
        return cmd;
    }

    public byte[] getBytes() {
        return bytes;
    }

	public short getSid() {
		return sid;
	}
}
