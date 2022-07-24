FROM golang:alpine3.14

WORKDIR /app

COPY go.mod ./
COPY go.sum ./


RUN mkdir -p /app/protos

COPY ./protos/* /app/protos
RUN ls /app
RUN ls /app/protos

RUN go mod download

COPY *.go ./

RUN go build -o /udf-uppercase-go

EXPOSE 55554

CMD [ "/udf-uppercase-go" ]