#!/bin/bash
java -jar lib/junit-platform-console-standalone-1.10.2.jar \
  --class-path "out/production:out/test:lib/*" \
  --scan-class-path out/test
