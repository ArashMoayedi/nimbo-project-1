package com.mycompany;


import com.google.common.util.concurrent.*;
import com.fasterxml.jackson.annotation.*;
import com.satori.rtm.*;
import com.satori.rtm.auth.*;
import com.satori.rtm.model.*;
import com.sun.org.apache.regexp.internal.RE;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static jdk.nashorn.internal.runtime.regexp.joni.Syntax.Java;

public class Program {
    static private final String endpoint = "wss://open-data.api.satori.com";
    static private final String appkey = "5c29c5bCf12c8f51c2276C46a48AC8be";
    // Role and secret are optional: replace only if you need to authenticate.
    static private final String role = "YOUR_ROLE";
    static private final String roleSecretKey = "YOUR_SECRET";
    static private final String channel = "github-events";

    static private HashMap<String, Integer> languageTenMinutesHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> languageHourHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> languageDayHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> repoNameTenMinutesHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> repoNameHourHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> repoNameDayHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> authorTenMinutesHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> authorHourHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> authorDayHashMap = new HashMap<String, Integer>();

    static private Map<String, Integer> sortedLanguageTenMinutesMap;
    static private Map<String, Integer> sortedLanguageHourMap;
    static private Map<String, Integer> sortedLanguageDayMap;
    static private Map<String, Integer> sortedRepoNameTenMinutesMap;
    static private Map<String, Integer> sortedRepoNameHourMap;
    static private Map<String, Integer> sortedRepoNameDayMap;
    static private Map<String, Integer> sortedAuthorTenMinutesHashMap;
    static private Map<String, Integer> sortedAuthorHourHashMap;
    static private Map<String, Integer> sortedAuthorDayHashMap;

    static private CountDownLatch latch = new CountDownLatch(0);



    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        final PrintWriter rawWriter = new PrintWriter("raw.txt");
        final RtmClientBuilder builder = new RtmClientBuilder(endpoint, appkey)
                .setListener(new RtmClientAdapter() {
                    @Override
                    public void onConnectingError(RtmClient client, Exception ex) {
                        String msg = String.format("RTM client failed to connect to '%s': %s",
                                endpoint, ex.getMessage());
                        System.out.println(msg);
                    }

                    @Override
                    public void onError(RtmClient client, Exception ex) {
                        String msg = String.format("RTM client failed: %s", ex.getMessage());
                        System.out.println(msg);
                    }

                    @Override
                    public void onEnterConnected(RtmClient client) {
                        System.out.println("Connected to Satori!");
                    }
                });

        //check if the role is set to authenticate or not
        boolean shouldAuthenticate = !"YOUR_ROLE".equals(role);
        if (shouldAuthenticate) {
            builder.setAuthProvider(new RoleSecretAuthProvider(role, roleSecretKey));
        }


        final RtmClient client = builder.build();

        System.out.println(String.format(
                "RTM connection config:\n" +
                        "\tendpoint='%s'\n" +
                        "\tappkey='%s'\n" +
                        "\tauthenticate?=%b", endpoint, appkey, shouldAuthenticate));

        client.start();

        InputThread inputThread = new InputThread();
        Thread t = new Thread(inputThread);
        t.start();

        TenThread tenThread = new TenThread();
        Thread tt = new Thread(tenThread);
        tt.start();




        // At this point, the client may not yet be connected to Satori RTM.
        // If the client is not connected, the SDK internally queues the subscription request and
        // will send it once the client connects
        client.createSubscription(channel, SubscriptionMode.SIMPLE,
                new SubscriptionAdapter() {
                    @Override
                    public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
                        // when subscription is established (confirmed by RTM)
                        System.out.println("Subscribed to the channel: " + channel);
                    }

                    @Override
                    public void onSubscriptionError(SubscriptionError error) {
                        // when a subscribe or subscription error occurs
                        System.out.println("Failed to subscribe: " + error.getReason());
                    }

                    @Override
                    public void onSubscriptionData(SubscriptionData data) {
                        // when incoming messages arrive
                        for (AnyJson json : data.getMessages()) {
                            try {
                                Event event = json.convertToType(Event.class);
                                String type = event.type;
                                rawWriter.append("hi");
                                rawWriter.flush();
                                latch.await();
                                String timeStamp = new SimpleDateFormat("yyMMddHHmmSS").format(new java.util.Date());
                                if (type.equals("ForkEvent")) {
                                    String repoName = event.repo.name;
                                    if (repoNameTenMinutesHashMap.containsKey(repoName)) {
                                        repoNameTenMinutesHashMap.put(repoName, repoNameTenMinutesHashMap.get(repoName) + 1);
                                    } else {
                                        repoNameTenMinutesHashMap.put(repoName, 1);
                                    }
                                } else if (type.equals("PushEvent")) {
                                    Commits[] commits = event.payload.commits;
                                    for (Commits commit : commits) {
                                        if (authorTenMinutesHashMap.containsKey(commit.author.name)) {
                                            authorTenMinutesHashMap.put(commit.author.name,
                                                    authorTenMinutesHashMap.get(commit.author.name) + 1);
                                        } else {
                                            authorTenMinutesHashMap.put(commit.author.name, 1);
                                        }

                                    }
                                } else if (type.equals("PullRequestEvent")) {
                                    String language = event.payload.pull_request.head.repo.language;
                                    if (languageTenMinutesHashMap.containsKey(language)) {
                                        languageTenMinutesHashMap.put(language, languageTenMinutesHashMap.get(language) + 1);
                                    } else if (!language.equals("null")) {
                                        languageTenMinutesHashMap.put(language, 1);
                                    }
                                }
                            } catch (Exception ignore) {
                            }
                        }
                    }
                });
    }

    static class Event {
        PayLoad payload;
        Repo repo;
        String type;
    }

    static class PayLoad {
        PullRequest pull_request;
        Commits[] commits;
    }

    static class Commits {
        Author author;
    }

    static class Author {
        String name;
    }

    static class PullRequest {
        Head head;
    }

    static class Head {
        Repo repo;
    }

    static class Repo {
        String language;
        String name;
    }

    static class InputThread implements Runnable {
        public void run() {
            System.out.println("Input your command:");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if (command.equals("top")) {
//                    printMap(sortedLanguageTenMinutesMap);
//                    printMap(sortedRepoNameTenMinutesMap);
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
                    latch = new CountDownLatch(1);
                    List<Map.Entry<String, Integer>> languageTenMinutesList =
                            new LinkedList<Map.Entry<String, Integer>>(languageTenMinutesHashMap.entrySet());

                    List<Map.Entry<String, Integer>> repoNameTenMinutesList =
                            new LinkedList<Map.Entry<String, Integer>>(repoNameTenMinutesHashMap.entrySet());

                    List<Map.Entry<String, Integer>> authorTenMinutesList =
                            new LinkedList<Map.Entry<String, Integer>>(authorTenMinutesHashMap.entrySet());


                    languageTenMinutesHashMap = new HashMap<String, Integer>();
                    repoNameTenMinutesHashMap = new HashMap<String, Integer>();
                    authorTenMinutesHashMap = new HashMap<String, Integer>();

                    latch.countDown();

                    sortedLanguageTenMinutesMap = sortByValue(languageTenMinutesList);
                    sortedRepoNameTenMinutesMap = sortByValue(repoNameTenMinutesList);
                    sortedAuthorTenMinutesHashMap = sortByValue(authorTenMinutesList);
                    printMap(sortedLanguageTenMinutesMap, "");
                    System.out.println("");
                    printMap(sortedRepoNameTenMinutesMap, "link");
                    System.out.println("");
                    printMap(sortedAuthorTenMinutesHashMap, "");
                    System.out.println("");
                } catch (InterruptedException e) {
                    System.out.println("TenThread interrupted");
                }
            }
        }
    }


    private static Map<String, Integer> sortByValue(List<Map.Entry<String, Integer>> list) {

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        Collections.reverse(list);
        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        /*
        //classic iterator example
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }*/


        return sortedMap;
    }

    static <K, V> void printMap(Map<K, V> map, String style) {
        int counter = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            counter++;
            if (style.equals("link")) {
                System.out.print("https://github.com/");
            }
            System.out.println(entry.getKey()
                    + " Value : " + entry.getValue());
            if(counter >= 10)
                break;
        }
    }
}