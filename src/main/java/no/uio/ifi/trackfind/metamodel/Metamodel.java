package no.uio.ifi.trackfind.metamodel;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;

@Data
public class Metamodel {

    private Multimap<String, String> analysisAttributes = HashMultimap.create();
    private Multimap<String, String> experimentAttributes = HashMultimap.create();
    private Multimap<String, String> ihecDataPortal = HashMultimap.create();
    private Multimap<String, String> otherAttributes = HashMultimap.create();
    private Multimap<String, String> sampleAttributes = HashMultimap.create();

}
