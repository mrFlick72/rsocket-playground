FROM openjdk:11

ADD rsocket-playground-publisher.jar /usr/local/rsocket-playground-publisher/

VOLUME /var/log/rsocket-playground-publisher

WORKDIR /usr/local/rsocket-playground-publisher/

CMD ["java",  "-jar", "rsocket-playground-publisher.jar"]