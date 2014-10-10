FROM clojure

MAINTAINER Simone Mosciatti <simone@mweb.biz>

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY project.clj /usr/src/app

RUN lein deps

COPY . /usr/src/app

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" ema-standalone.jar

CMD ["java", "-jar", "ema-standalone.jar"]
