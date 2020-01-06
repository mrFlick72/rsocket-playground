FROM openjdk:11

ADD rsocket-playground-consumer.jar /usr/local/rsocket-playground-consumer/

VOLUME /var/log/rsocket-playground-consumer

WORKDIR /usr/local/rsocket-playground-consumer/

CMD ["java",  "-jar", "rsocket-playground-consumer.jar"]