#!/usr/bin/env bash

DB=mysql
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JOOQ_VERSION=3.5.0
M2_REPOS=~/.m2/repository
MYSQL_JDBC_VERSION=5.1.35
PG_JDBC_VERSION=42.1.4
MVN=mvn

function usage() {
	echo 'gen.sh [-d <dbname>] [-m <path to maven>]'
}

while getopts ":d:m:" opt; do
	case $opt in
		d)
			DB=${OPTARG}
			;;

		m)
			MVN=${OPTARG}
			;;

		\?)
			usage
			exit 1
			;;
	esac
done

shift $(($OPTIND - 1))

JDBC_ARTIFACT=
JDBC_JAR=

if [ "$DB" == "mysql" ]; then
	JDBC_ARTIFACT="mysql:mysql-connector-java:$MYSQL_JDBC_VERSION"
	JDBC_JAR="$M2_REPOS/mysql/mysql-connector-java/$MYSQL_JDBC_VERSION/mysql-connector-java-$MYSQL_JDBC_VERSION.jar"
elif [ "$DB" == "postgres" ]; then
	JDBC_ARTIFACT="org.postgresql:postgresql:$PG_JDBC_VERSION"
	JDBC_JAR="$M2_REPOS/org/postgresql/postgresql/$PG_JDBC_VERSION/postgresql-$PG_JDBC_VERSION.jar"
else
	echo "Unsupported database type."
	exit 1
fi

JOOQ_JAR="$M2_REPOS/org/jooq/jooq/$JOOQ_VERSION/jooq-$JOOQ_VERSION.jar"
JOOQ_META_JAR="$M2_REPOS/org/jooq/jooq-meta/$JOOQ_VERSION/jooq-meta-$JOOQ_VERSION.jar"
JOOQ_CODEGEN_JAR="$M2_REPOS/org/jooq/jooq-codegen/$JOOQ_VERSION/jooq-codegen-$JOOQ_VERSION.jar"

if [ ! -e "$JDBC_JAR" ]; then
	$MVN org.apache.maven.plugins:maven-dependency-plugin:2.1:get -Dartifact=$JDBC_ARTIFACT -DrepoUrl=http://sonatype.org
fi

if [ ! -e "$JOOQ_JAR" ]; then
	$MVN org.apache.maven.plugins:maven-dependency-plugin:2.1:get -Dartifact=org.jooq:jooq:$JOOQ_VERSION -DrepoUrl=http://sonatype.org
fi

if [ ! -e "$JOOQ_META_JAR" ]; then
	$MVN org.apache.maven.plugins:maven-dependency-plugin:2.1:get -Dartifact=org.jooq:jooq-meta:$JOOQ_VERSION -DrepoUrl=http://sonatype.org
fi

if [ ! -e "$JOOQ_CODEGEN_JAR" ]; then
	$MVN org.apache.maven.plugins:maven-dependency-plugin:2.1:get -Dartifact=org.jooq:jooq-codegen:$JOOQ_VERSION -DrepoUrl=http://sonatype.org
fi


CLASSPATH="$JOOQ_JAR:$JOOQ_META_JAR:$JOOQ_CODEGEN_JAR:$JDBC_JAR:."

echo "Using classpath: $CLASSPATH"

java -cp "$CLASSPATH" org.jooq.util.GenerationTool "$DIR/gen.xml"
