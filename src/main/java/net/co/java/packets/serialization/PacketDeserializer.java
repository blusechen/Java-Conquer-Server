package net.co.java.packets.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import net.co.java.packets.IncomingPacket;
import net.co.java.packets.Packet;
import net.co.java.packets.PacketType;


public class PacketDeserializer {
	protected int totalStringLength;
	protected final IncomingPacket ip; 
	protected final Packet.PacketHeader ph; 
	
	protected PacketDeserializer(IncomingPacket ip, Packet.PacketHeader ph) {
		this.ip = ip; 
		this.ph = ph;
		//type = GenericUtility.getGenericType(this);
	}

    public PacketDeserializer(IncomingPacket ip) {
        this(ip, deserializeHeader(ip));
    }


	private static Packet.PacketHeader deserializeHeader(IncomingPacket ip) {
		Packet.PacketHeader ph = new Packet.PacketHeader();
		ph.setLength(ip.readUnsignedShort(0));
		ph.setType(PacketType.valueOf(ip.readUnsignedShort(2)));
		return ph;  
	}
	
	public Packet deserialize() throws DeserializationException {
		try {
			Class<?> clasz = Packets.getInstance().getPacketClass(ip.getPacketType());
			Packet packet = (Packet) clasz.getConstructor(IncomingPacket.class).newInstance(ip);
			packet.header = ph; 
			
			for(Field field : clasz.getDeclaredFields()) {
				if(field.isAnnotationPresent(PacketValue.class)) {
					if(field.isAnnotationPresent(Offset.class)){	
						PacketValue value = field.getAnnotation(PacketValue.class);
						Offset offset = field.getAnnotation(Offset.class);
						field.setAccessible(true);
					
						switch(value.type()) { 
							case ENUM_VALUE:
								setEnum(packet, field, value, offset);
								break;
							case STRING: 
								setString(packet, field, value, offset); 
								break;
							case STRING_WITH_LENGTH: 
								setStringWithLength(packet, field, value, offset);
							case UNSIGNED_BYTE:
							case BYTE:
							case UNSIGNED_SHORT: 
							case SHORT:
							case UNSIGNED_INT:
							case INT:
							default:
								setPrimitive(packet, field, value, offset);
								break;
						}	
					}
				}
			}
			
			return packet;
			
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
			throw new DeserializationException("Couldn't construct the packet, cause it has no noparameter constructor.");
		} catch (InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
			throw new DeserializationException("Couldn't construct the packet, It's not instantiable.");
		} catch (IllegalAccessException e) { /* Shouldn't be thrown as we set the fields with annotations accessible. */
			throw new RuntimeException("Coulnd't access the fields of the class. Use setAccessible()");
		} catch (Exception e) {
			throw new DeserializationException("Deserilization went wrong.");
		}
	}
	
	protected void setPrimitive(Object obj, Field field, PacketValue value, Offset offset) 
			throws IllegalArgumentException, IllegalAccessException { 
		switch(value.type()) { 
			case UNSIGNED_BYTE: 
				field.setShort(obj, ip.readUnsignedByte(offset.value() + totalStringLength));
				break;
			case UNSIGNED_SHORT:
				field.setInt(obj, ip.readUnsignedShort(offset.value() + totalStringLength));
				break;
			case UNSIGNED_INT: 
				field.setLong(obj, ip.readUnsignedInt(offset.value() + totalStringLength));
				break;
			default: 
				break;
		
		}
	}
	
	
	@SuppressWarnings({"unchecked", "rawtypes"})  //  Hard cast doesn't result in problems. 
	protected void setEnum(Object obj, Field field, PacketValue value, Offset offset)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException,
			SecurityException, DeserializationException, NoSuchMethodException, NoSuchFieldException { 
		Class<? extends Enum> enumClass = (Class<? extends Enum>) field.getType(); 
		Object o = EnumUtility.valueOf(enumClass, ip.readUnsignedShort(offset.value() + totalStringLength));
		field.set(obj, o);
	}
	
	protected void setString(Object obj, Field field, PacketValue value, Offset offset)
			throws IllegalArgumentException, IllegalAccessException { 
		field.set(obj, ip.readString(offset.value() + totalStringLength, 16));
		//totalStringLength += 16; 
	}
	
	protected void setStringWithLength(Object obj, Field field, PacketValue value, Offset offset) 
			throws IllegalArgumentException, IllegalAccessException {
        int length = ip.readUnsignedByte(offset.value() + totalStringLength);
        field.set(obj, ip.readString(offset.value() + totalStringLength + 1, length));
        totalStringLength += length;
    }
}
