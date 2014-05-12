#!/bin/bash
rm ../bin/*.class
javac -cp "C:\solr-4.7.1\dist\*;C:\solr-4.7.1\dist\solrj-lib\*" -d ../bin ../src/*.java
