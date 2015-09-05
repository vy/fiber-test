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

1. [Standard Java Threads](http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html)
2. [Akka Actors](http://akka.io/)
3. [Quasar Fibers and Actors](http://docs.paralleluniverse.co/quasar/)

Usage
-----

You first need to build the JMH Uber JAR using Maven.

    $ mvn clean install

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
    > com.github.vy.fibertest.QuasarFiberRingBenchmark

Results
-------

For `Oracle Java 1.8.0_60-b27` running on `Intel(R) Core(TM) i7-4702MQ CPU @ 2.20GHz` on `Linux 3.13.0-63-generic x86_64` kernel:

```
JavaThreadRingBenchmark.ringBenchmark      avgt   50  7741,274 ± 476,113  ms/op
QuasarActorRingBenchmark.ringBenchmark     avgt   50  1702,764 ±  65,940  ms/op
QuasarDataflowRingBenchmark.ringBenchmark  avgt   50  1512,901 ±  55,922  ms/op
QuasarChannelRingBenchmark.ringBenchmark   avgt   50  1417,803 ±  49,837  ms/op
AkkaActorRingBenchmark.ringBenchmark       avgt   50   909,057 ±  38,941  ms/op
QuasarFiberRingBenchmark.ringBenchmark     avgt   50   832,529 ±  47,586  ms/op
```

License
-------

The [fiber-test](https://github.com/vy/fiber-test/) by [Volkan Yazıcı](http://vlkan.com/) is licensed under the [Creative Commons Attribution 4.0 International License](http://creativecommons.org/licenses/by/4.0/).

[![Creative Commons Attribution 4.0 International License](http://i.creativecommons.org/l/by/4.0/80x15.png)](http://creativecommons.org/licenses/by/4.0/)
