package icircles.concreteDiagram;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class TestDatum {

    public String description;
    //public int decomp_strategy;
    //public int recomp_strategy;
    public double expected_checksum;

    public TestDatum(String string, 
            //int decomp_strategy,
            //int recomp_strategy, 
            double checksum) {
        description = string;
        //this.decomp_strategy = decomp_strategy;
        //this.recomp_strategy = recomp_strategy;
        expected_checksum = checksum;
    }

    /**
     * Return the description of the i'th TestDatum as a JSON string.
     * @param i The index into test_data
     * @return if i is out of bounds, return null, otherwise return a string in JSON format.
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder("{\"AbstractDiagram\":{\"Version\":0,");

        // Split the string into zones, shadedZones and spiders
        String [] components = Pattern.compile(",").split(description, 3);

        json.append(getContoursJSON(components[0]));

        json.append(',');
        json.append(getZonesJSON(components[0]));

        if(components.length >= 2) {
            json.append(',');
            json.append(getShadedZonesJSON(components[1]));

            if(components.length >= 3) {
                json.append(',');
                json.append(getSpidersJSON(components[2]));
            } else { // empty spiders
                json.append(',');
                json.append("\"Spiders\" : []");
            }
        } else { // empty shaded zones and spiders
            json.append(',');
            json.append("\"ShadedZones\" : []");
            json.append(',');
            json.append("\"Spiders\" : []");
        }
        json.append("}}");
        return json.toString();
    }

    private StringBuilder getContoursJSON(String zones) {
        StringBuilder json = new StringBuilder("\"Contours\":");
        // Remove spaces from the zones specification
        zones = zones.replaceAll(" ", "");
        // Remove duplicate characters from the zones specification
        Set<Character>  zs = new HashSet<Character>(Arrays.asList(ArrayUtils.toObject(zones.toCharArray())));

        json.append(collectionToJSONArray(zs));
        return json;
    }

    private StringBuilder getZonesJSON(String zones) {
        StringBuilder json = new StringBuilder("\"Zones\":");

        // Build the zones as a set of strings of the form {"in": ["a","b"]}
        Set <String> zs          = new HashSet<String>();
        zs.add("{\"in\": []}"); // default outside zone
        String []    individuals = Pattern.compile(" ").split(zones);
        for(String z : individuals) {
            StringBuilder sb = new StringBuilder("{\"in\":");
            sb.append(collectionToJSONArray(Arrays.asList(ArrayUtils.toObject(z.toCharArray()))));
            sb.append("}");
            zs.add(sb.toString());
        }

        json.append(collectionToJSONArray(zs, false));
        return json;
    }

    private StringBuilder getShadedZonesJSON(String zones) {
        StringBuilder json = new StringBuilder("\"ShadedZones\":");

        // Build the zones as a set of strings of the form {"in": ["a","b"]}
        Set <String> zs          = new HashSet<String>();
        if(zones.contains(".")) {
            zs.add("{\"in\":[]}"); // outside zone
            zones = zones.replaceAll("\\.", ""); // Match a '.' char
        }

        // if we do any further pattern matching on "", we'll end up with
        // strange results.  i.e. the result of "".split(" ") is an array
        // containing the empty string
        if("".equals(zones)) {
            json.append(collectionToJSONArray(zs, false));
            return json;
        }

        String []    individuals = Pattern.compile(" ").split(zones);
        for(String z : individuals) {
            StringBuilder sb = new StringBuilder("{\"in\":");
            sb.append(collectionToJSONArray(Arrays.asList(ArrayUtils.toObject(z.toCharArray()))));
            sb.append("}");
            zs.add(sb.toString());
        }

        json.append(collectionToJSONArray(zs, false));
        return json;
    }

    private StringBuilder getSpidersJSON(String spiders) {
        StringBuilder json = new StringBuilder("\"Spiders\":");

        // Use a List here as we can have many spiders with the same (name, habitat) pair
        List<String> ss = new Vector<String>();
        // Individuals are space separated zone descriptions
        String [] individuals = Pattern.compile(",").split(spiders);
        for(String s : individuals) {
            // trim leading spaces
            s = StringUtils.stripStart(s, " ");

            StringBuilder sp = new StringBuilder();
            String [] habitat = Pattern.compile(" ").split(s);

            // if the last element in habitat begins with a single quote char,
            // then it is the label for a spider.  Otherwise it is a zone
            // description
            sp.append("{\"name\":");
            if('\'' == habitat[habitat.length - 1].charAt(0)) {
                String label = habitat[habitat.length - 1].substring(1);
                sp.append("\"" + label + "\""); // the label in quotes

                // remove the label from the habitat arrary
                // expensive :(
                List<String> ls =Arrays.asList(habitat);
                habitat = (ls.subList(0, ls.size() - 1)).toArray(new String[0]);
            } else {
                sp.append("null"); // the JSON null object (unquoted)
            }
            sp.append(", \"habitat\":");

            HashSet<String> hs = new HashSet<String>();
            for(String z : habitat) {
                if(".".equals(z)) { //e outside zone
                    hs.add("{\"in\":[]}");
                } else {
                    StringBuilder sb = new StringBuilder("{\"in\":");
                    sb.append(collectionToJSONArray(Arrays.asList(ArrayUtils.toObject(z.toCharArray()))));
                    sb.append("}");
                    hs.add(sb.toString());
                }
            }
            sp.append(collectionToJSONArray(hs, false));
            sp.append("}"); // close array of habitats and (name, habitat) object
            ss.add(sp.toString());
        }

        json.append(collectionToJSONArray(ss, false));
        return json;
    }

    private StringBuilder collectionToJSONArray(Collection c) {
        return collectionToJSONArray(c, true);
    }

    private StringBuilder collectionToJSONArray(Collection c, boolean withQuotes) {
        StringBuilder s    = new StringBuilder("[");
        Iterator      iter = c.iterator();
        while(iter.hasNext()) {
            if(withQuotes) {
                s.append("\"");
            }
            s.append(iter.next().toString());
            if(withQuotes) {
                s.append("\"");
            }
            if (!iter.hasNext()) {
                break;                  
              }
            s.append(",");
        }

        s.append("]");
        return s;
    }
}
