// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package grpc

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/harness/harness-core/commons/go/lib/logs"
	pb "github.com/harness/harness-core/product/ci/scm/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

func TestParseWebhookErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewSCMHandler(stopCh, log.Sugar())
	_, err := h.ParseWebhook(ctx, &pb.ParseWebhookRequest{})
	assert.NotNil(t, err)
}

func TestIsLatestFilePositivePath(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/FileFindSource.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.IsLatestFileRequest{
		Slug:   "tphoney/scm-test",
		Path:   "jello",
		BlobId: "980a0d5f19a64b4b30a87d4206aade58726b60e3",
		Type: &pb.IsLatestFileRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewSCMHandler(stopCh, log.Sugar())
	got, err := h.IsLatestFile(context.Background(), in)

	assert.Nil(t, err, "no errors")
	assert.True(t, got.Latest, "status matches")
}
