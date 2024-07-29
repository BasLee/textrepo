FROM alpine:3.19

ARG DOCKER_TAG
ENV DOCKER_TAG $DOCKER_TAG

ARG SOURCE_COMMIT
ENV SOURCE_COMMIT $SOURCE_COMMIT

RUN apk add --no-cache gettext

WORKDIR /about
COPY ./scripts/start.sh /about
COPY ./src /about/src

CMD ["./start.sh"]
