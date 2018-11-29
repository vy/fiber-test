[![License](https://img.shields.io/github/license/vy/fiber-test.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

This project bundles a set of benchmarks that aims to measure the performance
of a multitude of threading implementations in the Java Virtual Machine.

# Benchmarks

- **Ring**: `N` spawned threads are connected in a ring structure.
  Through this ring a message (an integer) is circulated `M` times. (Adopted
  from the [Performance Measurements of Threads in Java and Processes in
  Erlang](http://web.archive.org/web/20150906052630/https://www.sics.se/%7ejoe/ericsson/du98024.html)
  article.)

Implementations
---------------

Project employs 3 threading libraries to implement fibers in the benchmarks.

1. [Standard Java Threads](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Thread.html)
2. [Akka Actors](https://akka.io/)
3. [Quasar Fibers and Actors](https://docs.paralleluniverse.co/quasar/)
4. [Kilim Actors, Continuations, and Fibers](https://github.com/kilim/kilim)
5. [Project Loom Continuations and Fibers](https://openjdk.java.net/projects/loom/)

Usage
-----

You first need to build the JMH Uber JAR using Maven.

    $ JAVA_11_HOME=/path/to/java-11/home mvn package

Next, you can either use the provided `benchmark.sh` script

    $ ./benchmark.sh --help
    Available parameters (with defaults):
        workerCount (503)
        ringSize    (1000000)
        cpuList     (0-1)

    # You can run with default parameters.
    $ ./benchmarks.sh

    # Alternatively, you can configure parameters through environment variables.
    $ ringSize=500 workerCount=30 cpuList=0-7 ./benchmark.sh

or call JMH manually:

    $ java \
    > -jar target/fiber-test.jar \
    > -jvmArgsAppend "-DworkerCount=503 -DringSize=1000000 -javaagent:/path/to/quasar-core-<version>.jar" \
    > -wi 5 -i 10 -bm avgt -tu ms -f 5 \
    > ".*RingBenchmark.*"

Instead of using JMH, you can additionally use `assembly:single` goal to
create an all-in-one JAR and run benchmarks individually.

    $ mvn assembly:single
    $ java \
    > -server -XX:+TieredCompilation -XX:+AggressiveOpts \
    > -DworkerCount=503 -DringSize=10000000 \
    > -javaagent:/path/to/quasar-core-<version>.jar \
    > -cp target/fiber-test-<version>-jar-with-dependencies.jar \
    > com.vlkan.fibertest.ring.QuasarFiberRingBenchmark

Results
-------

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

Contributors
------------

- [Arek Burdach](https://github.com/arkadius)
- [Seth Lytle](https://github.com/nqzero)

License
-------

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
