package ca.utoronto.msrg.padres.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Message;

public class Utils {

	/**
	 * To find out the size of an object in bytes. The object must be serializable.
	 * 
	 * @param object
	 *            The object whose size is to be found.
	 * @return The size of the objects in number of bytes
	 * @throws IOException
	 *             If there is an error in object serialization
	 */
	public static long getSerializedObjectSize(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(object);
		oos.flush();
		oos.close();
		bos.close();
		byte[] data = bos.toByteArray();
		return data.length;
	}

	/**
	 * Concatenate two arrays of type T and produce a new of the same type. The first array is
	 * inserted first followed by the second.
	 * 
	 * @param <T>
	 * @param first
	 *            An array of type T with arbitrary length
	 * @param second
	 *            An array of type T with arbitrary length
	 * @return An array of T, resulted from the concatenation of the given two arrays
	 */
	public static <T> T[] arrayConcat(T[] first, T[] second) {
		T[] combinedArray = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, combinedArray, first.length, second.length);
		return combinedArray;
	}

	public static boolean checkAllTrue(boolean[] checkArray) {
		for (boolean check : checkArray)
			if (!check)
				return false;
		return true;
	}

	/**
	 * An utiltiy method to convert a list of messages (regardless of their type) into a human
	 * readable text block.
	 * 
	 * @param msgList
	 *            The message list to be converted.
	 * @return A human readable text block listing the given message list.
	 */
	public static String messageListToString(List<? extends Message> msgList) {
		String outString = "";
		for (Message msg : msgList) {
			outString += String.format("[%s] %s\n", msg.getMessageID(), msg);
		}
		return outString;
	}

	/**
	 * An utiltiy method to convert a message map of String -> Message (regardless of their type)
	 * into a human readable text block.
	 * 
	 * @param msgList
	 *            The message map to be converted.
	 * @return A human readable text block listing the given message map.
	 */
	public static String messageMapToString(Map<String, ? extends Message> msgMap) {
		String outString = "";
		String[] idList = msgMap.keySet().toArray(new String[0]);
		Arrays.sort(idList);
		for (String msgID : idList) {
			outString += String.format("[%s] %s\n", msgID, msgMap.get(msgID));
		}
		return outString;
	}

}
