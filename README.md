# AUR Cache Client (Java)

This project provides a simple command line tool and library for interacting
with the [multi-level cache service](https://github.com/NikolayNN/multi-level-cache-service).
It uses `java.net.http.HttpClient` and Jackson for JSON serialization.

## Building

The project is built with Maven. Run:

```bash
mvn package
```

The resulting JAR will be placed in `target/aur-cache-client-java-1.0-SNAPSHOT.jar`.

## Command Line Usage

```
java -jar target/aur-cache-client-java-1.0-SNAPSHOT.jar <baseUrl> <command> [args]
```

### Commands

- `put <key> <value>` – store a value
- `get <key>` – retrieve a value
- `evict <key>` – remove a value
- `getAll` – fetch all entries
- `putAll <key1> <value1> [<key2> <value2> ...]` – store multiple entries
- `evictAll` – clear the cache

Set the compression threshold for outgoing requests using the `gzipThreshold`
system property or the `GZIP_THRESHOLD` environment variable. Values larger
than the threshold are gzipped before being sent.

## Library Usage

`CacheClient` can also be used programmatically:

```java
CacheClient client = new CacheClient("http://localhost:8080", 1024);
client.put("k", Map.of("foo", 1));
MyValue v = client.get("k", MyValue.class);
```

`get` and `getAll` return objects of the provided class using Jackson
conversion.
