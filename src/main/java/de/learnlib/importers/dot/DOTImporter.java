package de.learnlib.importers.dot;


import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.SimpleAlphabet;

import javax.annotation.WillClose;
import java.io.*;
import java.util.*;

/**
 * Utility methods to import (gzipped) DOT files,
 *
 * @author Falk Howar
 *
 */
public class DOTImporter {


    public static CompactMealy<String, String> read(@WillClose InputStream is) throws IOException {

        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        LinkedHashMap<String,Integer> states = new LinkedHashMap<>();
        List<String[]> transitions = new ArrayList<>();

        // read file
        String line;
        while (null != (line = r.readLine())) {
            line = line.trim();
            if (line.contains("->")) {
                transitions.add(line.split(" "));
            }
            else if (line.contains("s") && line.contains("label") && !line.contains("__")) {
                String id = line.substring(0, line.indexOf(" "));
                states.put(id, states.size());
            }
        }
        r.close();

        // pre-processing and inputs
        HashSet<String> inputs = new HashSet<>();
        for (String[] t : transitions) {
            //System.out.println(Arrays.toString(t));
            t[0] = t[0].trim();
            t[2] = t[2].trim();
            if (t.length > 3) {
                t[3] = t[3].substring(t[3].indexOf("\"")+1).trim();
                t[5] = t[5].substring(0, t[5].indexOf("\"")).trim();
                inputs.add(t[3]);
            }
            //System.out.println(Arrays.toString(t));
        }

        // basic automaton
        Alphabet<String> sigma = new SimpleAlphabet<>(inputs);
        CompactMealy<String, String> mealy = new CompactMealy<>(sigma);
        for (int i=0; i< states.size(); i++) {
            mealy.addState();
        }

        // transitions
        for (String[] t : transitions) {
            if ("__start0".equals(t[0])) {
                mealy.setInitialState(states.get(t[2].replace(";", "")));
                continue;
            }
            //System.out.println("T: " + t[0] + " : " + t[3]  + " : " + t[2] + " : " + t[5]);
            mealy.setTransition(states.get(t[0]), t[3], states.get(t[2]), t[5]);
        }

        if (mealy.getInitialStates().isEmpty()) {
            mealy.setInitialState(states.get("s0"));
        }

        return mealy;
    }
}
