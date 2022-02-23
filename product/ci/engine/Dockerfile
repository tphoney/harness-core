# Create image for ci-lite-engine which will orchestrate steps in a stage
#
# Build the ci-engine docker image using:
# > bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/ci/engine/...
# > docker build -t harness/ci-lite-engine:<tag> -f product/ci/engine/Dockerfile $(bazel info bazel-bin)/product/ci/engine/

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.5-230
RUN microdnf update
RUN microdnf install shadow-utils
RUN apk add git
COPY engine_/engine /usr/local/bin/ci-lite-engine
RUN chmod +x /usr/local/bin/ci-lite-engine

RUN adduser -g harness -s /bin/sh -D -u 1000 harness
USER harness:harness

CMD ["/usr/local/bin/ci-lite-engine"]