package com.mycompany;

import java.io.*;
import java.util.*;

import static com.mycompany.DataMap.printMap;
import static com.mycompany.DataMap.sortByValue;

public class Analysis {

    private static class AnalysisRequest {

        long from;
        long to;
        String request;

        AnalysisRequest(long from, long to, String request) {

            this.from = from;
            this.to = to;
            this.request = request;
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null) {
                return false;
            }
            if (!AnalysisRequest.class.isAssignableFrom(obj.getClass())) {
                return false;
            }
            final AnalysisRequest other = (AnalysisRequest) obj;
            return this.from == other.from;
        }

        private ArrayList<Integer> indexOfAll(ArrayList list) {
            ArrayList<Integer> indexList = new ArrayList<Integer>();
            for (int i = 0; i < list.size(); i++)
                if (this.equals(list.get(i)))
                    indexList.add(i);
            return indexList;
        }

        int bestIndex(ArrayList<AnalysisRequest> list) {
            ArrayList<Integer> indexList = this.indexOfAll(list);
            if (indexList.size() == 0) {
                return -1;
            }
            int bestIndex = 0;
            for (int index : indexList) {
                if (list.get(index).to <= this.to &&
                        list.get(index).to > list.get(bestIndex).to) {
                    bestIndex = index;
                }
            }
            return bestIndex;
        }
    }

    private static ArrayList<AnalysisRequest> analysisRequestArrayList = new ArrayList<AnalysisRequest>();

    static void dataAnalysis(String from, String to, String request) throws IOException {

        HashMap<String, Integer> resultMap = new HashMap<String, Integer>();
        BufferedReader dataReader = null;
        AnalysisRequest newRequest = null;
        String line;
        boolean caughtException = false;
        try {
            from = from.replaceAll("[^\\d]", "");
            to = to.replaceAll("[^\\d]", "");
            long start = Long.parseLong(from);
            long end = Long.parseLong(to);
            for (; start <= end; start++) {
                try {
                    newRequest = new AnalysisRequest(start, end, request);
                    int bestIndex = newRequest.bestIndex(analysisRequestArrayList);
                    if (bestIndex != -1) {

                        dataReader = new BufferedReader(new FileReader(analysisRequestArrayList.get(bestIndex).from + "-" +
                                analysisRequestArrayList.get(bestIndex).to + "-" + request));

                        start = analysisRequestArrayList.get(bestIndex).to;
                    } else {

                        dataReader = new BufferedReader(new FileReader(request + "/" + Long.toString(start)));
                    }
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
                analysisRequestArrayList.add(newRequest);
            }
            List<Map.Entry<String, Integer>> resultList =
                    new LinkedList<Map.Entry<String, Integer>>(resultMap.entrySet());
            printMap(sortByValue(resultList), "", new File(from + "-" + to + "-" + request));
        } catch (Exception ignore) {
            System.out.println("Invalid inputs");
        }
    }

}

