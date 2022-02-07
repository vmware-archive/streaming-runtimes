package main

import (
	"context"
	"log"
	"net"
	"strings"

	pb "github.com/tzolov/poc-go-grpc/message"

	"google.golang.org/grpc"
)

const (
	port = ":55554"
)

// server is used to implement message.MessagingServiceServer.
type server struct {
	pb.UnimplementedMessagingServiceServer
}

// RequestReply implements message.MessagingServiceServer#RequestReply
func (s *server) RequestReply(ctx context.Context, in *pb.GrpcMessage) (*pb.GrpcMessage, error) {
	log.Printf("Received: %v", string(in.Payload))
	upperCasePayload := strings.ToUpper(string(in.Payload))
	return &pb.GrpcMessage{Payload: []byte(upperCasePayload)}, nil
}

func main() {
	listen, err := net.Listen("tcp", port)
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	newServer := grpc.NewServer()
	pb.RegisterMessagingServiceServer(newServer, &server{})
	log.Printf("server listening at %v", listen.Addr())
	if err := newServer.Serve(listen); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
