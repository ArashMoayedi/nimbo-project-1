package com.mycompany;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;

public class DataMap {
    HashMap<String, Integer> hashMap;


    public static Map<String, Integer> sortByValue(List<Map.Entry<String, Integer>> list) {

        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Collections.reverse(list);

        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();

        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }


    static <K, V> void printMap(Map<K, V> map, String style, File outputFile){

        try {
            final PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8")));
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (style.equals("link")) {
                    writer.append("https://github.com/");
                }
                writer.append(String.valueOf(entry.getKey())).append("^").append(String.valueOf(entry.getValue())).append("\n");
                writer.flush();

            }

            writer.close();
        } catch (Exception ignored) {

        }

    }
}
