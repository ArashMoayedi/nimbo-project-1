package com.mycompany;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.mycompany.DataMap.printMap;
import static com.mycompany.DataMap.sortByValue;

public class Analysis {


    static void dataAnalysis(String from, String to, String need) throws IOException {

        HashMap<String, Integer> resultMap = new HashMap<String, Integer>();
        BufferedReader dataReader = null;
        String line;
        boolean caughtException = false;
        try {
            from = from.replaceAll("[^\\d]", "");
            to = to.replaceAll("[^\\d]", "");
            long start = Long.parseLong(from);
            long end = Long.parseLong(to);
            for (; start <= end; start++) {
                try {
                    dataReader = new BufferedReader(new FileReader(need + "/" + Long.toString(start)));
                } catch (FileNotFoundException e) {
                    if (!caughtException) {
                        System.out.println("Not enough data, continue analysing? [y/n]");
                        Scanner scanner = new Scanner(System.in);
                        String command = scanner.nextLine();
                        if (command.equals("n")) {
                            System.out.println("Aborted!");
                            break;
                        }
                        caughtException = true;
                    }
                }
                while ((line = dataReader.readLine()) != null) {
                    String[] parts = line.split("\\^", 2);
                    if (parts.length >= 2) {
                        String key = parts[0];
                        int value = Integer.parseInt(parts[1]);
                        if (resultMap.containsKey(key)) {
                            resultMap.put(key, resultMap.get(key) + value);
                        } else {
                            resultMap.put(key, value);
                        }
                    } else {
                        System.out.println("ignoring line: " + line);
                    }
                }
            }
            List<Map.Entry<String, Integer>> resultList =
                    new LinkedList<Map.Entry<String, Integer>>(resultMap.entrySet());
            printMap(sortByValue(resultList), "", new File(from + "-" + to + "-" + need));
        } catch (Exception ignore) {
            System.out.println("Invalid inputs");
        }
    }

}

