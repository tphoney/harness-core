module github.com/harness/harness-core/product/ci/scm

go 1.14

replace github.com/harness/harness-core/commons/go/lib => ../../../commons/go/lib

replace github.com/harness/harness-core/product/log-service => ../../../product/log-service

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/drone/go-scm v1.29.2-0.20230515092809-5877f3846131
	github.com/drone/go-scm-codecommit v0.0.0-20210315104920-2d8b9dc5ed8a
	github.com/go-git/go-git/v5 v5.4.2
	github.com/golang/mock v1.6.0
	github.com/golang/protobuf v1.5.2
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/harness/harness-core v0.0.0-20230207125731-25cf0db6b130 // indirect
	github.com/harness/harness-core/commons/go/lib v0.0.0-20220222141117-7659b7eca599
	github.com/mattn/go-zglob v0.0.4 // indirect
	github.com/stretchr/testify v1.8.1
	github.com/wings-software/autogen-go v1.0.0
	go.uber.org/zap v1.15.0
	golang.org/x/net v0.5.0
	google.golang.org/grpc v1.53.0
	google.golang.org/protobuf v1.28.1
	mvdan.cc/sh/v3 v3.2.4 // indirect
)
