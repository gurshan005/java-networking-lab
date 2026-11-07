# Java Networking Lab

This repository contains a Java routing algorithm by Gurshan Bhogal and a new localhost socket exercise. The related team projects below show how the same networking concepts were used in a bulletin board and a file transfer protocol.

## Run the examples

With JDK 17 or later installed, run the following from the repository root.

```sh
./verify.sh
```

The script compiles to a temporary directory and runs both examples. It uses only the Java standard library, binds sockets to loopback, and requires no external server.

### Equal-cost routing

[RoutingDemo.java](src/RoutingDemo.java) adapts the shortest-path and first-hop calculations from Gurshan’s individual Java routing implementation. Dijkstra’s algorithm retains all equal-cost predecessors. The forwarding calculation propagates those choices back to the first hop. A gateway can be a destination, and paths stop expanding when they reach it.

The seven-router example checks three outcomes.

| Destination from router 1 | Cost | First hops | Reason |
| --- | ---: | --- | --- |
| Router 4 | 2 | 2, 3 | Both two-link paths have the same cost. |
| Router 6 | 13 | 2 | A shorter path would pass through gateway 4, where the route must end. |
| Router 7 | Unreachable | None | No link reaches this router. |

### TCP and UDP loopback

[LoopbackCheck.java](src/LoopbackCheck.java) is a new companion exercise written for this portfolio. It sends the same seeded 1 MiB payload over TCP and UDP and checks every received byte. The UDP example labels 1,024 blocks with sequence numbers and withholds the first acknowledgment for every fifth block. The sender retries after a timeout. The receiver acknowledges the retry and appends each block only once.

An example run produced the following output.

```text
TCP: 1,048,576 bytes verified
UDP: 1,048,576 bytes verified, 204 ACKs withheld, 204 retransmissions
```

## Related team projects

Gurshan contributed to both projects with classmates. The results below come from the original team implementations.

- **Concurrent TCP bulletin board.** A Java server uses a fixed thread pool to serve clients and a read/write lock around shared notes. A local check connected two clients at once, posted a note from one, retrieved it by colour from the other, and disconnected both cleanly.
- **Reliable file transfer over UDP.** The team compared Stop-and-Wait and Go-Back-N across 72 recorded trials covering two file sizes, three acknowledgment-loss settings, and four protocol configurations. In a separate localhost check, all four configurations recovered the same 1 MiB file byte-for-byte while the receiver dropped every fifth acknowledgment. The Go-Back-N window sizes were 20, 40, and 80 packets.
