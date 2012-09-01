package mikera.tyrant.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Iterator;

/**
 *
 * @author  Carsten Muessig <carsten.muessig@gmx.net>
 */

public class MetaData {
    
    private TreeMap<String, MetaDataEntry> metaDataEntries; // key = property name, value = MetaDataEntry
    
    protected MetaData() {
        metaDataEntries = new TreeMap<>();
    }
    
    protected MetaData(MetaData parent) {
        this();
        if(parent!=null)
            metaDataEntries.putAll(parent.getAll());
    }
    
    protected void add(String propertyName, MetaDataEntry property) {
        metaDataEntries.put(propertyName, property);
    }
    
    protected void add(String propertyName, Object value, Object[] validValues, int valueCondition, int propertyCondition) {
        add(propertyName, new MetaDataEntry(value, validValues, valueCondition, propertyCondition));
    }
    
    protected TreeMap<String, MetaDataEntry> getAll() {
        return metaDataEntries;
    }
    
   protected MetaDataEntry get(String property) {
        return metaDataEntries.get(property);
    }
    
    protected int numberOfMandatoryProperties() {
        int number = 0;
        Iterator<String> it = metaDataEntries.keySet().iterator();
        while(it.hasNext()) {
            MetaDataEntry tmd = metaDataEntries.get(it.next());
            if(tmd.isMandatory())
                number++;
        }
        return number;
    }
    
    protected boolean describes(Map<String, Object> properties, boolean isMetaData) {
        int mandatoryPropertiesChecked = 0;
        Set<String> propertyNames = properties.keySet();
        if(metaDataEntries.keySet().containsAll(propertyNames)) {
            Iterator<String> it = propertyNames.iterator();
            while(it.hasNext()) {
                String propertyName = it.next();
                if(isMetaData)
                    System.out.println("   Checking meta data property \""+propertyName+"\"");
                else
                    System.out.println("   Checking property \""+propertyName+"\"");
                MetaDataEntry mde = metaDataEntries.get(propertyName);
                if((mde!=null)&&(mde.describes(properties.get(propertyName)))) {
                    if(mde.isMandatory())
                        mandatoryPropertiesChecked++;
                    if(isMetaData)
                        System.out.println("   Meta data property \""+propertyName+"\" successful checked");
                    else
                        System.out.println("   Property \""+propertyName+"\" successful checked");
                } else {
                    if(isMetaData)
                        System.out.println("   Meta data property \""+propertyName+"\" not successful checked.");
                    else
                        System.out.println("   Property \""+propertyName+"\" not successful checked.");
                }
            }
        }
        return mandatoryPropertiesChecked==numberOfMandatoryProperties();
    }
}