package com.mycompany;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import static com.mycompany.DataMap.printMap;
import static com.mycompany.Program.*;

public class Theards {

    static class InputThread implements Runnable {
        public void run() {
            System.out.println("Input your command:");
            Scanner scanner = new Scanner(System.in);
            while (true) {

                String from = scanner.nextLine();
                String to = scanner.nextLine();
                String need = scanner.nextLine();
                try {
                    Analysis.dataAnalysis(from, to, need);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }


    static class TenThread implements Runnable {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(60000);
                    String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date());
                    latch = new CountDownLatch(1);

                    latch.countDown();

                    File languageFile = new File(languageDir, timeStamp);
                    printMap(languageMinuteHashMap, "", languageFile);

                    File repoFile = new File(repoDir, timeStamp);
                    printMap(repoNameMinuteHashMap, "link", repoFile);

                    File authorFile = new File(authorDir, timeStamp);
                    printMap(authorMinuteHashMap, "", authorFile);

                    File userFile = new File(userDir, timeStamp);
                    printMap(userMinuteHashMap, "link", userFile);


                } catch (InterruptedException e) {
                    System.out.println("TenThread interrupted");
                }
            }
        }

    }

}
