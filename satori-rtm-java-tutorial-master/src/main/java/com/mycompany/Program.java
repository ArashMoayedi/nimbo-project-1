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

import java.util.*;
import java.util.concurrent.*;

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
    static private HashMap<String, Integer> idTenMinutesHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> idHourHashMap = new HashMap<String, Integer>();
    static private HashMap<String, Integer> idDayHashMap = new HashMap<String, Integer>();

    static private Map<String, Integer> sortedLanguageTenMinutesMap;
    static private Map<String, Integer> sortedLanguageHourMap;
    static private Map<String, Integer> sortedLanguageDayMap;
    static private Map<String, Integer> sortedIdTenMinutesMap;
    static private Map<String, Integer> sortedIdHourMap;
    static private Map<String, Integer> sortedIdDayMap;

    public static void main(String[] args) throws InterruptedException {
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
                                String language = event.payload.pull_request.head.repo.language;
                                String id = event.id;
                                if(languageTenMinutesHashMap.containsKey(language)){
                                    languageTenMinutesHashMap.put(language, languageTenMinutesHashMap.get(language) + 1);
                                }
                                else if(!languageTenMinutesHashMap.containsKey(language) && !language.equals("null")){
                                    languageTenMinutesHashMap.put(language, 1);
                                }
                                if(idTenMinutesHashMap.containsKey(id)){
                                    idTenMinutesHashMap.put(id, idTenMinutesHashMap.get(id) + 1);
                                }
                                else if(!idTenMinutesHashMap.containsKey(id)){
                                    idTenMinutesHashMap.put(id, 1);
                                }
                            } catch (Exception ignored) {

                            }
                        }
                    }
                });


//    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//    // configure timed task to publish a message each 2 seconds
//    executor.scheduleWithFixedDelay(new Runnable() {
//      public void run() {
//        double lat = 34.134358 + Math.random();
//        double lon = -118.321506 + Math.random();
//        final Animal animal = new Animal("zebra", new double[]{lat, lon});
//
//        // At this point, the client may not yet be connected to Satori RTM.
//        // If the client is not connected, the SDK internally queues the publish request and
//        // will send it once the client connects
//        ListenableFuture<Pdu<PublishReply>> reply = client.publish(channel, animal, Ack.YES);
//
//        Futures.addCallback(reply, new FutureCallback<Pdu<PublishReply>>() {
//          public void onSuccess(Pdu<PublishReply> publishReplyPdu) {
//            System.out.println("Animal is published: " + animal);
//          }
//
//          public void onFailure(Throwable t) {
//            System.out.println("Publish request failed: " + t.getMessage());
//          }
//        });
//      }
//    }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Sample model to publish to RTM.
     *
     * This class represents the following raw json structure:
     * <pre>{@literal
     * {
     *   "who": "zebra",
     *   "where": [34.134358, -118.321506]
     * }
     * }</pre>
     */
    static class Event {
        PayLoad payload;
        String id;
    }
    static class PayLoad{
        PullRequest pull_request;
    }
    static class PullRequest{
        Head head;
    }
    static class Head{
        Repo repo;
    }
    static class Repo{
        String language;
    }

    static class InputThread implements Runnable {
        public void run() {
            System.out.println("Input your command:");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if (command.equals("top 1")) {
                    printMap(sortByValue(sortedIdTenMinutesMap));
                }
                if (command.equals("top 2")) {
                    printMap(sortByValue(sortedLanguageTenMinutesMap));
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
                    sortedLanguageTenMinutesMap = sortByValue(languageTenMinutesHashMap);
                    sortedIdTenMinutesMap = sortByValue(idTenMinutesHashMap);
                    languageTenMinutesHashMap = new HashMap<String, Integer>();
                    idTenMinutesHashMap = new HashMap<String, Integer>();
                } catch (InterruptedException e) {
                    System.out.println("TenThread interrupted");
                }
            }
        }
    }


    private static Map<String, Integer> sortByValue (Map<String, Integer> unsortMap) {

        // 1. Convert Map to List of Map
        List<Map.Entry<String, Integer>> list =
                new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

        // 2. Sort list with Collections.sort(), provide a custom Comparator
        //    Try switch the o1 o2 position for a different order
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

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

    static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println("Key : " + entry.getKey()
                    + " Value : " + entry.getValue());
        }
    }


}