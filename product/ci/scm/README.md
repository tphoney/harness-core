# Using SCM

## Running in vscode / debug mode

1. This assumes you can run the following commands successfully from the root of `./portal`.

    ```BASH
    bazelisk build //product/ci/scm/proto/...
    bazelisk build //product/ci/scm/...
    gazelle
    ```

2. git change.

    ```BASH
    cat ~/.gitconfig
    [url "git@github.com:"]
      insteadOf = https://github.com/
    ```

3. go private EG `~/,zshrc`

    ```BASH
    export GOPRIVATE=github.com/harness/harness-core
    ```

4. Only open the scm folder in vscode `portal/product/ci/scm`, do not open from the portal root.
5. You will need to comment out the logservice line from `commons/go/lib/go.mod` Then you can run go build in `portal/product/ci/scm` or run the vscode debugger.
6. If you change the proto file you will need to run `bazelisk build //product/ci/scm/proto/...` to re-create the go file `scm.pb.go`. Then you can copy this file to `portal/product/ci/scm`.

    ```BASH
    cp /home/tp/.cache/bazel/_bazel_tp/529a9f5eb5d3c3de90f20271ededd500/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin/product/ci/scm/proto/linux_amd64_stripped/ciscmpb_go_proto%/github.com/harness/harness-core/product/ci/scm/proto/scm.pb.go ~/workspace/portal/product/ci/scm/proto
    ```

7. If you change code dependencies you will need to re-run gazelle again to update the `BUILD.bazel` files in `portal/product/ci/scm`.

```BASH
diff --git a/commons/go/lib/go.mod b/commons/go/lib/go.mod
index bdfe753b01..2bef85919d 100644
--- a/commons/go/lib/go.mod
+++ b/commons/go/lib/go.mod
@@ -30,7 +30,7 @@ require (
        github.com/sourcegraph/jsonrpc2 v0.0.0-20200429184054-15c2290dcb37 // indirect
        github.com/stretchr/testify v1.6.1
        github.com/vdemeester/k8s-pkg-credentialprovider v1.18.1-0.20201019120933-f1d16962a4db
-       github.com/harness/harness-core/product/log-service v0.0.0-00010101000000-000000000000
+//     github.com/harness/harness-core/product/log-service v0.0.0-00010101000000-000000000000
        go.uber.org/zap v1.15.0
        golang.org/x/tools v0.0.0-20201105220310-78b158585360 // indirect
        google.golang.org/api v0.24.0
```

## Building the SCM binary setting version and build commit ID

To ensure we know what build the binary is using and what proto file was used. We include a version number and the short commit ID. These are included in stdout when the binary starts.

```BASH
go build -ldflags "-X 'main.Version=$VERSION' -X 'main.BuildCommitID=$(git rev-parse --short HEAD)'"
```

giving ...

```BASH
{"level":"info","ts":"2021-04-16T09:20:43.261+0100","caller":"scm/main.go:53","msg":"Starting CI GRPC scm server","application_name":"CI-scm","deployable":"ci-scm","deployment":"","environment":"dev","version":"1.1.0","buildCommitID":"1f51565c5a","port":8091,"unixSocket":""}
```

## Running acceptance tests

To run the unit tests

```BASH
go test -count=1 -v ./...
```

### Github

uses `https://github.com/tphoney/scm-test`, **remove the test_branch branch. Remove the pr that is using the patch1 branch**

```BASH
GITHUB_ACCESS_TOKEN=963408579168567c07ff8bfd2a5455e5307f74d4 go test -count=1 -v ./...
```

### Bitbucket cloud

uses `https://bitbucket.org/tphoney/scm-test`, **remove the branch test_branch**

```BASH
BITBUCKET_CLOUD_TOKEN=JWhLv8FqXHs7mEP32hbR go test -count=1 -v ./...
```

### Gitlab

uses `https://gitlab.com/tphoney/test_repo`, **remove the branch test_branch**

```BASH
GITLAB_ACCESS_TOKEN=-uABtgnSCDpuemgkSwgY go test -count=1 -v ./...
```

## Running SCM binary with a unix socket and using the test client

How to build the scm binary and use a unix socket then run an example test client.

```BASH
# build the scm binary
bazelisk build //product/ci/scm/...
# where it is located
ls -al $(bazelisk info bazel-bin)/product/ci/scm/*stripped/scm
INFO: Invocation ID: 69ecbe65-f101-469e-a5b9-4d39f25a426b
-r-xr-xr-x 1 tp tp 13149393 Apr 12 11:38 /home/tp/.cache/bazel/_bazel_tp/529a9f5eb5d3c3de90f20271ededd500/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin/product/ci/scm/linux_amd64_stripped/scm
# run the scm binary using a unix socket, remove the socket first
rm /tmp/bla
$(bazelisk info bazel-bin)/product/ci/scm/*stripped/scm --unix=/tmp/bla
 ls -al /tmp/bla
srwxr-xr-x 1 tp tp 0 Apr 12 11:46 /tmp/bla


# build the test unix_socket_client
bazelisk build //product/ci/scm/test/unix_socket_client/...
# where it is located
ls -al $(bazelisk info bazel-bin)/product/ci/scm/test/unix_socket_client/*stripped/unix_socket_client
INFO: Invocation ID: 1a900e85-523f-4bf0-b0bd-37ce9ed6aa55
-r-xr-xr-x 1 tp tp 10119187 Apr 12 11:38 /home/tp/.cache/bazel/_bazel_tp/529a9f5eb5d3c3de90f20271ededd500/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin/product/ci/scm/test/unix_socket_client/linux_amd64_stripped/unix_socket_client
# run the test unix_socket_client
$(bazelisk info bazel-bin)/product/ci/scm/test/unix_socket_client/*stripped/unix_socket_client
INFO: Invocation ID: 1b31454f-5079-4f3b-9a52-9c132013e6c1
content: content:"# scm-test\ntest repo for source control operations\n" path:"README.md" blob_id:"81e158a64f10351f15a17e9c3888f06101855eca" %
```

## Example requests

I tested using grpc calls using `https://github.com/uw-labs/bloomrpc`

### File requests

```BASH
// find
{
  "slug": "tphoney/scm-test",
  "path": "README.md",
  "branch": "main",
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
```
