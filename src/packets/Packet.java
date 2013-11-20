package packets;

import java.io.IOException;

import conquerServer.ServerThread;

public abstract class Packet {
	PacketType packetType;
	byte[] data;
	int offset = 0;
	
	public Packet(PacketType packetType) {
		this.packetType = packetType;
	}
	
	public int getPacketSize() {
		return packetType.size;
	}

	public PacketType getPacketType() {
		return packetType;
	}

	public static Packet route(byte[] data, ServerThread thread) throws IOException
	{
		PacketType packetType = PacketType.get((data[3] & 0xFF) << 8 | (data[2] & 0xFF));
		return create(packetType, data, thread);
	}
	
	public static Packet create(PacketType packetType, ServerThread thread) throws IOException
	{
		return create(packetType, new byte[packetType.size], thread);
	}
	
	public static Packet create(PacketType packetType, byte[] data, ServerThread thread) throws IOException
	{
		switch(packetType){
			case AUTH_LOGIN_PACKET: return new Auth_Login_Packet(packetType, data, thread);
			case AUTH_LOGIN_RESPONSE: return new Auth_Login_Response(packetType, data, thread);
			case CHAR_INFO_PACKET:
			case GENERAL_DATA_PACKET:
			default: return null;
		}		
	}

}