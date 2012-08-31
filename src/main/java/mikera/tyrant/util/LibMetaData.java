package mikera.tyrant.util;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * @author Carsten Muessig <carsten.muessig@gmx.net>
 */

public class LibMetaData {

	private static LibMetaData instance = null;
	private TreeMap<String, MetaData> metaData; // key = thing name, value = MetaData of thing

	private LibMetaData() {
		metaData = new TreeMap<String, MetaData>();
	}

	protected static LibMetaData instance() {
		if (instance == null) {
			instance = new LibMetaData();
			LibMetaDataHandler.createLibMetaData(instance);
		}
		return instance;
	}

	protected void add(String thingName, MetaData thing) {
		metaData.put(thingName, thing);
	}

	protected TreeMap<String, MetaData> getAll() {
		return metaData;
	}

	protected MetaData get(String thingName) {
		return metaData.get(thingName);
	}

	protected String describes(TreeMap item) {
		ArrayList<String> metaDataNames = new ArrayList<String>();
		Iterator<String> it = metaData.keySet().iterator();
		while (it.hasNext()) {
			String libItemName = it.next();
			MetaData libItem = metaData.get(libItemName);
			if (libItem.describes(item, false))
				metaDataNames.add(libItemName);
		}
		if (metaDataNames.size() == 0)
			System.out.println(" The item doesn't match any library meta data");
		if (metaDataNames.size() == 1) {
			System.out.println(" The item matches the library meta data \""	+ metaDataNames.get(0) + "\"");
			return metaDataNames.get(0);
		}
		if (metaDataNames.size() > 1) {
			System.out.println(" The item is ambiguous, "
					+ metaDataNames.size()
					+ " matching descriptions were found: " + metaDataNames);
			System.out
					.println(" Reasons: Faulty plug-in data or faulty meta data inside the program");
			System.out
					.println(" Please contact the Tyrant developers if you're not sure and provide your plug-in file");
		}
		return null;
	}

	public static boolean isKnownProperty(String property) {
		TreeMap<String, MetaData> metaData = instance().getAll();
		Iterator<String> it = metaData.keySet().iterator();
		while (it.hasNext()) {
			String thingName = it.next();
			MetaData meta = metaData.get(thingName);
			if (meta.get(property) != null)
				return true;
		}
		return false;
	}

	public static boolean isValidProperty(String property, Object value) {
		TreeMap<String, MetaData> metaData = instance().getAll();
		Iterator<String> it = metaData.keySet().iterator();
		MetaDataEntry med = null;
		while (it.hasNext()) {
			String thingName = it.next();
			MetaData meta = metaData.get(thingName);
			if (meta.get(property) != null) {
				System.out.println(meta.getAll().keySet());
				med = meta.get(property);
				System.out.println(med.getValue());
				break;
			}
		}
		if (med != null)
			return med.describes(value);
		System.out.println("No meta data for "+property+" available");
		return false;
	}
	
	public static String getPropertyDescription(String property) {
		return (String)LibMetaDataHandler.createPropertyDescriptions().get(property);
	}
}