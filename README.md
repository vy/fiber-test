[![License](https://img.shields.io/github/license/vy/fiber-test.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

This project bundles a set of benchmarks that aims to measure the performance
of a multitude of threading implementations in the Java Virtual Machine.

Benchmarks
----------

As of now, there is just a single benchmark (ring) adopted from the
[Performance Measurements of Threads in Java and Processes in
Erlang](http://www.sics.se/~joe/ericsson/du98024.html) article. I plan to
introduce new benchmarks as time allows. That being said, contributions are
welcome.

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
    > com.vlkan.fibertest.QuasarFiberRingBenchmark

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

for `Java(TM) SE Runtime Environment (build 1.8.0_121-b13)` running on `Intel(R) Core(TM) i3-2105 CPU @ 3.10GHz` on `Linux 4.4.0-138-generic x86_64` kernel:
```
KilimActorRingBenchmark.Fork.ringBenchmark    avgt    4  656.668 ± 1488.584  ms/op
KilimActorRingBenchmark.ringBenchmark         avgt    4  492.612 ±   57.829  ms/op
KilimContinuationRingBenchmark.ringBenchmark  avgt    4   34.866 ±    3.135  ms/op
KilimFiberRingBenchmark.Fork.ringBenchmark    avgt    4  493.886 ±  218.133  ms/op
KilimFiberRingBenchmark.ringBenchmark         avgt    4  402.553 ±   97.912  ms/op
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
