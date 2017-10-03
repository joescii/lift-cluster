#!/usr/bin/env bash

if [ $# -eq 0 ]; then
  PUBLISH=publishSigned
else
  PUBLISH=$1
fi

publish() {
  LIFT_VERSION="set liftVersion in ThisBuild := \"$1\""
  CROSS_SCALA="set crossScalaVersions := Seq($2)"

  sbt "$LIFT_VERSION" "$CROSS_SCALA" \
    clean "+ update" "+ test" \
    "+ lift-cluster-common/$PUBLISH" \
    "+ lift-cluster-kryo/$PUBLISH" \
    "+ lift-cluster-jetty9/$PUBLISH"
}

publish "3.2.0-SNAPSHOT" '"2.11.11"'
