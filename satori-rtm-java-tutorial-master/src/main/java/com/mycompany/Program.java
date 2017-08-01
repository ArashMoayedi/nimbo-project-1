package com.mycompany;


import com.satori.rtm.*;
import com.satori.rtm.auth.*;
import com.satori.rtm.model.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Program {
    static private final String endpoint = "wss://open-data.api.satori.com";
    static private final String appkey = "5c29c5bCf12c8f51c2276C46a48AC8be";
    // Role and secret are optional: replace only if you need to authenticate.
    static private final String role = "YOUR_ROLE";
    static private final String roleSecretKey = "YOUR_SECRET";
    static private final String channel = "github-events";

    static private HashMap<String, Integer> languageMinuteHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> repoNameMinuteHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> authorMinuteHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> userMinuteHashMap = new HashMap<String, Integer>();

    static private Map<String, Integer> sortedLanguageMinuteMap;
    static private Map<String, Integer> sortedRepoNameMinuteMap;
    static private Map<String, Integer> sortedAuthorMinuteHashMap;
    static private Map<String, Integer> sortedUserMinuteHashMap;


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
                                latch.await();
                                String login = event.actor.login;
                                if (userMinuteHashMap.containsKey(login)) {
                                    int size = 1;
                                    if(type.equals("PushEvent")){
                                        size =event.payload.commits.length;
                                    }
                                    if(size != 0){
                                        userMinuteHashMap.put(login, userMinuteHashMap.get(login) + 1);
                                    }
                                } else {
                                    userMinuteHashMap.put(login, 1);
                                }
                                if (type.equals("ForkEvent") || type.equals("WatchEvent") || type.equals("IssueEvent")) {
                                    String repoName = event.repo.name;
                                    if (repoNameMinuteHashMap.containsKey(repoName)) {
                                        if (type.equals("ForkEvent")) {
                                            repoNameMinuteHashMap.put(repoName, repoNameMinuteHashMap.get(repoName) + 3);
                                        } else if (type.equals("WatchEvent")) {
                                            repoNameMinuteHashMap.put(repoName, repoNameMinuteHashMap.get(repoName) + 2);
                                        } else if (type.equals("IssueEvent")) {
                                            repoNameMinuteHashMap.put(repoName, repoNameMinuteHashMap.get(repoName) + 1);
                                        }
                                    } else {
                                        if (type.equals("ForkEvent")) {
                                            repoNameMinuteHashMap.put(repoName, 3);
                                        } else if (type.equals("WatchEvent")) {
                                            repoNameMinuteHashMap.put(repoName, 2);
                                        } else if (type.equals("IssueEvent")) {
                                            repoNameMinuteHashMap.put(repoName, 1);
                                        }
                                    }
                                } else if (type.equals("PushEvent")) {
                                    Commits[] commits = event.payload.commits;
                                    for (Commits commit : commits) {
                                        if (authorMinuteHashMap.containsKey(commit.author.email)) {
                                            authorMinuteHashMap.put(commit.author.email,
                                                    authorMinuteHashMap.get(commit.author.email) + 1);
                                        } else {
                                            authorMinuteHashMap.put(commit.author.email, 1);
                                        }
                                    }
                                } else if (type.equals("IssueEvent")) {
                                    String login2 = event.payload.issue.user.login;
                                    if (userMinuteHashMap.containsKey(login2)) {
                                        userMinuteHashMap.put(login2, userMinuteHashMap.get(login2) + 1);
                                    } else {
                                        userMinuteHashMap.put(login2, 1);
                                    }

                                } else if (type.equals("PullRequestEvent")) {
                                    String language = event.payload.pull_request.head.repo.language;
                                    if (languageMinuteHashMap.containsKey(language)) {
                                        languageMinuteHashMap.put(language, languageMinuteHashMap.get(language) + 1);
                                    } else if (!language.equals("null")) {
                                        languageMinuteHashMap.put(language, 1);
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
        Actor actor;
    }


    static class Actor {
        String login;
    }

    static class PayLoad {
        PullRequest pull_request;
        Commits[] commits;
        Issue issue;

    }

    static class Issue {
        User user;
    }

    static class User {
        String login;
    }

    static class Commits {
        Author author;
    }

    static class Author {
        String email;
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
//                    printMap(sortedLanguageMinuteMap);
//                    printMap(sortedRepoNameMinuteMap);
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
                    List<Map.Entry<String, Integer>> languageMinuteList =
                            new LinkedList<Map.Entry<String, Integer>>(languageMinuteHashMap.entrySet());

                    List<Map.Entry<String, Integer>> repoNameMinuteList =
                            new LinkedList<Map.Entry<String, Integer>>(repoNameMinuteHashMap.entrySet());

                    List<Map.Entry<String, Integer>> authorMinuteList =
                            new LinkedList<Map.Entry<String, Integer>>(authorMinuteHashMap.entrySet());
                    List<Map.Entry<String, Integer>> userMinuteList =
                            new LinkedList<Map.Entry<String, Integer>>(userMinuteHashMap.entrySet());

                    languageMinuteHashMap = new HashMap<String, Integer>();
                    repoNameMinuteHashMap = new HashMap<String, Integer>();
                    authorMinuteHashMap = new HashMap<String, Integer>();
                    userMinuteHashMap = new HashMap<String, Integer>();

                    latch.countDown();

                    sortedLanguageMinuteMap = sortByValue(languageMinuteList);
                    sortedRepoNameMinuteMap = sortByValue(repoNameMinuteList);
                    sortedAuthorMinuteHashMap = sortByValue(authorMinuteList);
                    sortedUserMinuteHashMap = sortByValue(userMinuteList);

                    printMap(sortedLanguageMinuteMap, "");

                    printMap(sortedRepoNameMinuteMap, "link");

                    printMap(sortedAuthorMinuteHashMap, "");

                    printMap(sortedUserMinuteHashMap, "link");

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
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
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

    static <K, V> void printMap(Map<K, V> map, String style)  {
        int counter = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            counter++;
            if (style.equals("link")) {
                System.out.print("https://github.com/");
            }
            System.out.println(entry.getKey() + ": " + entry.getValue());

            if (counter >= 10)
                break;
        }
        System.out.println("");
    }
}