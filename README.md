hypercli-java
=============

Java API client for Hyper_

# Build

## Compile
```
$ mvn compile
```

## Test

> src/test/java/sh.hyper.hyperjava.api.model/Hyper*Test.java

```
//set env
$ export HYPER_ACCESS_KEY=ZLOZKxxxxxxxxxxxxxxxxAG5E
$ export HYPER_SECRET_KEY=4zRPAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxaZKOxB

//compile + test
$ mvn test

//compile + specify test
$ mvn test -Dtest=HyperVersionTest
$ mvn test -Dtest=Hyper*Test
```

## Package

output: `./target/docker-java-3.0.0.jar`

```
//compile + test + package
$ mvn package

//compile + package (faster)
$ mvn package -DskipTests
```

## Install

Installing `./target/docker-java-3.0.0.jar` to `~/.m2/repository/com/github/docker-java/docker-java/3.0.0/docker-java-3.0.0.jar`

```
//compile + test + package + install
$ mvn install

//compile + package + install (faster)
$ mvn install -DskipTests
```
