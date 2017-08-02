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

    static HashMap<String, Integer> languageMinuteHashMap = new HashMap<String, Integer>();
    static HashMap<String, Integer> repoNameMinuteHashMap = new HashMap<String, Integer>();
    static HashMap<String, Integer> authorMinuteHashMap = new HashMap<String, Integer>();
    static HashMap<String, Integer> userMinuteHashMap = new HashMap<String, Integer>();

    static CountDownLatch latch = new CountDownLatch(0);

    static File languageDir = new File("languages");
    static File authorDir = new File("authors");
    static File repoDir = new File("repos");
    static File userDir = new File("users");

    public static void main(String[] args) throws InterruptedException, FileNotFoundException {

        File logDir = new File("logs");
        logDir.mkdir();
        final PrintWriter logWriter = new PrintWriter(new FileOutputStream(
                new File(logDir, "log.txt"),
                true
        ));
        authorDir.mkdir();
        languageDir.mkdir();
        repoDir.mkdir();
        userDir.mkdir();


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

        Theards.InputThread inputThread = new Theards.InputThread();
        Thread t = new Thread(inputThread);
        t.start();

        Theards.TenThread tenThread = new Theards.TenThread();
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
                                String timeStamp = new SimpleDateFormat("yyyyMMddHHmmSS").format(new java.util.Date());
                                logWriter.append(timeStamp).append(" : Type: ").append(type);
                                String login = event.actor.login;
                                logWriter.append(" Actor_Login: ").append(login);
                                if (userMinuteHashMap.containsKey(login)) {
                                    int size = 1;
                                    if (type.equals("PushEvent")) {
                                        size = event.payload.commits.length;
                                    }
                                    if (size != 0) {
                                        userMinuteHashMap.put(login, userMinuteHashMap.get(login) + 1);
                                    }
                                } else {
                                    userMinuteHashMap.put(login, 1);
                                }
                                if (type.equals("ForkEvent") || type.equals("WatchEvent") || type.equals("IssueEvent")) {
                                    String repoName = event.repo.name;
                                    logWriter.append(" Repo_Name: ").append(repoName);
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
                                    logWriter.append(" Authors: ");
                                    for (Commits commit : commits) {
                                        logWriter.append("Name: ").append(commit.author.name).append(" Email: ").append(commit.author.email).append(" || ");
                                        if (authorMinuteHashMap.containsKey(commit.author.email)) {
                                            authorMinuteHashMap.put(commit.author.email,
                                                    authorMinuteHashMap.get(commit.author.email) + 1);
                                        } else {
                                            authorMinuteHashMap.put(commit.author.email, 1);
                                        }
                                    }
                                } else if (type.equals("IssueEvent")) {
                                    String login2 = event.payload.issue.user.login;
                                    logWriter.append("Action: ").append(event.payload.action);
                                    if (userMinuteHashMap.containsKey(login2)) {
                                        userMinuteHashMap.put(login2, userMinuteHashMap.get(login2) + 1);
                                    } else {
                                        userMinuteHashMap.put(login2, 1);
                                    }

                                } else if (type.equals("PullRequestEvent")) {
                                    String language = event.payload.pull_request.head.repo.language;
                                    logWriter.append(" Language: ").append(language);
                                    if (languageMinuteHashMap.containsKey(language)) {
                                        languageMinuteHashMap.put(language, languageMinuteHashMap.get(language) + 1);
                                    } else if (!language.equals("null")) {
                                        languageMinuteHashMap.put(language, 1);
                                    }
                                }
                                logWriter.append("\n");
                            } catch (Exception ignore) {
                            }
                        }
                    }
                });
    }
}