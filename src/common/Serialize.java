package common;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Serialize {
	// static Logger logger = Logger.getLogger("");

	public static void writeObject(String path, Serializable obj)
			throws IOException {
		FileOutputStream f_out = new FileOutputStream(path);
		ObjectOutputStream obj_out = new ObjectOutputStream(f_out);
		obj_out.writeObject(obj);
		f_out.close();

	}

	public static Object readObject(String path) throws IOException {
		FileInputStream f_in = new FileInputStream(path);
		ObjectInputStream obj_in = new ObjectInputStream(f_in);
		Object obj = null;
		try {
			obj = obj_in.readObject();
		}
		catch (ClassNotFoundException e) {
			// logger.fatal("Cannot recognize object's class: " + path);
			System.out.println("Cannot recognize object's class: " + path);
		}
		f_in.close();
		return obj;

	}

}
