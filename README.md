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

1. [Standard Java Threads](https://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html)
2. [Akka Actors](https://akka.io/)
3. [Quasar Fibers and Actors](https://docs.paralleluniverse.co/quasar/)
4. [Kilim Fibers](https://github.com/nqzero/kilim)

Usage
-----

You first need to build the JMH Uber JAR using Maven.

    $ mvn clean package

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

For `Oracle Java 1.8.0_60-b27` running on `Intel(R) Core(TM) i7-4702MQ CPU @ 2.20GHz` on `Linux 3.13.0-63-generic x86_64` kernel:

```
JavaThreadRingBenchmark.ringBenchmark      avgt   50  7741,274 ± 476,113  ms/op
QuasarActorRingBenchmark.ringBenchmark     avgt   50  1702,764 ±  65,940  ms/op
QuasarDataflowRingBenchmark.ringBenchmark  avgt   50  1512,901 ±  55,922  ms/op
QuasarChannelRingBenchmark.ringBenchmark   avgt   50  1417,803 ±  49,837  ms/op
AkkaActorRingBenchmark.ringBenchmark       avgt   50   892,533 ±  50,865  ms/op
QuasarFiberRingBenchmark.ringBenchmark     avgt   50   832,529 ±  47,586  ms/op
```

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
