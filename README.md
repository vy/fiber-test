[![License](https://img.shields.io/github/license/vy/fiber-test.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

A bundle of benchmarks measuring the performance of various fiber (lightweight
thread, actor, coroutine, etc.) implementations for the JVM.

# Benchmarks

- **Ring**: `N` spawned threads are connected in a ring structure.
  Through this ring a message (an integer) is circulated `M` times. (Adopted
  from the [Performance Measurements of Threads in Java and Processes in
  Erlang](http://web.archive.org/web/20150906052630/https://www.sics.se/%7ejoe/ericsson/du98024.html)
  article.)

# Implementations

1. [Standard Java Threads](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Thread.html)
2. [Akka Actors](https://akka.io/)
3. [Quasar Fibers and Actors](https://docs.paralleluniverse.co/quasar/)
4. [Kilim Actors, Continuations, and Fibers](https://github.com/kilim/kilim)
5. [Project Loom Continuations and Fibers](https://openjdk.java.net/projects/loom/)

# Usage

You first need to build the necessary [JMH](https://openjdk.java.net/projects/code-tools/jmh/)
fat JARs and then let the benchmark script do its job:

    $ ./mvnw package
    $ ./benchmark/benchmark.py

If you don't have a local JDK 11 installation, you can also use the shipped
`docker.sh` to compile and run the benchmark in a Docker container:

    $ ./docker.sh mvn-package
    $ ./docker.sh benchmark

# Results

For `Oracle Java 1.8.0_121-b13` running on `Intel(R) Core(TM) i3-2105 CPU @ 3.10GHz` on `Linux 4.4.0-137-generic x86_64` kernel:

```
Benchmark                                     Mode  Cnt     Score      Error  Units
AkkaActorRingBenchmark.ringBenchmark          avgt    4   614.755 ±  151.874  ms/op
JavaThreadRingBenchmark.ringBenchmark         avgt    4  6126.367 ±  304.880  ms/op
KilimActorRingBenchmark.ringBenchmark         avgt    4   611.655 ±  128.317  ms/op
KilimContinuationRingBenchmark.ringBenchmark  avgt    4    45.544 ±   11.303  ms/op
KilimFiberRingBenchmark.ringBenchmark         avgt    4   441.433 ±   61.134  ms/op
QuasarActorRingBenchmark.ringBenchmark        avgt    4  2627.271 ± 2114.870  ms/op
QuasarChannelRingBenchmark.ringBenchmark      avgt    4  1589.085 ±  819.561  ms/op
QuasarDataflowRingBenchmark.ringBenchmark     avgt    4  1866.151 ±  302.077  ms/op
QuasarFiberRingBenchmark.ringBenchmark        avgt    4   666.120 ±  148.967  ms/op
```

# Contributors

- [Arek Burdach](https://github.com/arkadius)
- Jordan Sheinfeld
- [Seth Lytle](https://github.com/nqzero)

# License

Copyright &copy; 2014-2018 [Volkan Yazıcı](http://vlkan.com/)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
