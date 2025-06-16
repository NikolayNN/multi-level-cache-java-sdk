package com.aurora.cache.client;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -jar client.jar <baseUrl> <command> [args]\n" +
                    "Commands:\n" +
                    "  put <key> <value>\n" +
                    "  get <key>\n" +
                    "  evict <key>\n" +
                    "  getAll\n" +
                    "  putAll <key1> <value1> [<key2> <value2> ...]\n" +
                    "  evictAll\n" +
                    "Environment variable GZIP_THRESHOLD or system property gzipThreshold sets compression threshold in bytes");
            return;
        }

        String baseUrl = args[0];
        String command = args[1];
        int threshold = Integer.parseInt(System.getProperty("gzipThreshold", System.getenv().getOrDefault("GZIP_THRESHOLD", "0")));
        CacheClient client = new CacheClient(baseUrl, threshold);
        try {
            switch (command) {
                case "put" -> {
                    if (args.length < 4) {
                        System.out.println("put requires <key> <value>");
                        return;
                    }
                    client.put(args[2], args[3]);
                    System.out.println("Stored");
                }
                case "get" -> {
                    if (args.length < 3) {
                        System.out.println("get requires <key>");
                        return;
                    }
                    String value = client.get(args[2], String.class);
                    System.out.println(value);
                }
                case "evict" -> {
                    if (args.length < 3) {
                        System.out.println("evict requires <key>");
                        return;
                    }
                    client.evict(args[2]);
                    System.out.println("Evicted");
                }
                case "getAll" -> {
                    java.util.Map<String, String> all = client.getAll(String.class);
                    all.forEach((k, v) -> System.out.println(k + "=" + v));
                }
                case "putAll" -> {
                    if (args.length < 4 || args.length % 2 != 0) {
                        System.out.println("putAll requires pairs of <key> <value>");
                        return;
                    }
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    for (int i = 2; i < args.length; i += 2) {
                        map.put(args[i], args[i + 1]);
                    }
                    client.putAll(map);
                    System.out.println("Stored batch");
                }
                case "evictAll" -> {
                    client.evictAll();
                    System.out.println("Cache cleared");
                }
                default -> System.out.println("Unknown command: " + command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
