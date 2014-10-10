FROM clojure

MAINTAINER Simone Mosciatti <simone@mweb.biz>

RUN mkdir -p /usr/src/ema
WORKDIR /usr/src/ema
COPY project.clj /usr/src/ema

RUN lein deps

COPY . /usr/src/ema

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" ema-standalone.jar

CMD ["java", "-jar", "ema-standalone.jar"]
