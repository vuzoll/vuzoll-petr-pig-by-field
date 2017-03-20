FROM openjdk:8

RUN mkdir -p /usr/src/vuzoll-petr-pig-by-field && mkdir -p /usr/app

COPY build/distributions/* /usr/src/vuzoll-petr-pig-by-field/

RUN unzip /usr/src/vuzoll-petr-pig-by-field/vuzoll-petr-pig-by-field-*.zip -d /usr/app/ && ln -s /usr/app/vuzoll-petr-pig-by-field-* /usr/app/vuzoll-petr-pig-by-field

WORKDIR /usr/app/vuzoll-petr-pig-by-field

ENTRYPOINT ["./bin/vuzoll-petr-pig-by-field"]
CMD []
