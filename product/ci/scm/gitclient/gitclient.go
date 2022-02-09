// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package gitclient

import (
	"crypto/tls"
	"fmt"
	"net/http"
	"net/http/httputil"

	"github.com/drone/go-scm/scm"
	"github.com/drone/go-scm/scm/driver/bitbucket"
	"github.com/drone/go-scm/scm/driver/gitea"
	"github.com/drone/go-scm/scm/driver/github"
	"github.com/drone/go-scm/scm/driver/gitlab"
	"github.com/drone/go-scm/scm/driver/stash"
	"github.com/drone/go-scm/scm/transport"

	"github.com/drone/go-scm/scm/transport/oauth2"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func oauthTransport(token string, skip bool) http.RoundTripper {
	return &oauth2.Transport{
		Base: defaultTransport(skip),
		Source: oauth2.StaticTokenSource(
			&scm.Token{
				Token: token,
			},
		),
	}
}
func privateTokenTransport(token string, skip bool) http.RoundTripper {
	return &transport.PrivateToken{
		Base:  defaultTransport(skip),
		Token: token,
	}
}

func giteaTransport(token string, skip bool) http.RoundTripper {
	return &oauth2.Transport{
		Base:   defaultTransport(skip),
		Scheme: oauth2.SchemeBearer,
		Source: oauth2.StaticTokenSource(
			&scm.Token{
				Refresh: token,
				Token:   token,
			},
		),
	}
}

func bitbucketTransport(username, password string, skip bool) http.RoundTripper {
	return &transport.BasicAuth{
		Base:     defaultTransport(skip),
		Username: username,
		Password: password,
	}
}

// defaultTransport provides a default http.Transport. If skip verify is true, the transport will skip ssl verification.
func defaultTransport(skip bool) http.RoundTripper {
	return &http.Transport{
		Proxy: http.ProxyFromEnvironment,
		TLSClientConfig: &tls.Config{
			InsecureSkipVerify: skip, //nolint:gosec //TLS skip is set in the grpc request.
		},
	}
}

func GetValidRef(p pb.Provider, inputRef, inputBranch string) (string, error) {
	if inputRef != "" {
		return inputRef, nil
	} else if inputBranch != "" {
		switch p.GetHook().(type) {
		case *pb.Provider_BitbucketCloud:
			return inputBranch, nil
		case *pb.Provider_BitbucketServer:
			return inputBranch, nil
		default:
			return scm.ExpandRef(inputBranch, "refs/heads"), nil
		}
	} else {
		return "", status.Error(codes.InvalidArgument, "Must provide a ref or a branch")
	}
}

func GetGitClient(p pb.Provider, log *zap.SugaredLogger) (client *scm.Client, err error) { //nolint:gocyclo,funlen
	switch p.GetHook().(type) {
	case *pb.Provider_Github:
		if p.GetEndpoint() == "" {
			client = github.NewDefault()
		} else {
			client, err = github.New(p.GetEndpoint())
			if err != nil {
				log.Errorw("GetGitClient failure Github", "endpoint", p.GetEndpoint(), zap.Error(err))
				return nil, err
			}
		}
		var token string
		switch p.GetGithub().GetProvider().(type) {
		case *pb.GithubProvider_AccessToken:
			token = p.GetGithub().GetAccessToken()
		default:
			return nil, status.Errorf(codes.Unimplemented, "Github Application not implemented yet")
		}
		client.Client = &http.Client{
			Transport: oauthTransport(token, p.GetSkipVerify()),
		}
	case *pb.Provider_Gitlab:
		if p.GetEndpoint() == "" {
			client = gitlab.NewDefault()
		} else {
			client, err = gitlab.New(p.GetEndpoint())
			if err != nil {
				log.Errorw("GetGitClient failure Gitlab", "endpoint", p.GetEndpoint(), zap.Error(err))
				return nil, err
			}
		}
		var token string
		switch p.GetGitlab().GetProvider().(type) {
		case *pb.GitlabProvider_AccessToken:
			token = p.GetGitlab().GetAccessToken()
			client.Client = &http.Client{
				Transport: oauthTransport(token, p.GetSkipVerify()),
			}
		case *pb.GitlabProvider_PersonalToken:
			token = p.GetGitlab().GetPersonalToken()
			client.Client = &http.Client{
				Transport: privateTokenTransport(token, p.GetSkipVerify()),
			}
		default:
			return nil, status.Errorf(codes.Unimplemented, "Gitlab provider not implemented yet")
		}

	case *pb.Provider_Gitea:
		if p.GetEndpoint() == "" {
			log.Error("getGitClient failure Gitea, endpoint is empty")
			return nil, status.Errorf(codes.InvalidArgument, fmt.Sprintf("Must provide an endpoint for %s", p.String()))
		}
		client, err = gitea.New(p.GetEndpoint())
		if err != nil {
			log.Errorw("GetGitClient failure Gitea", "endpoint", p.GetEndpoint(), zap.Error(err))
			return nil, err
		}
		client.Client = &http.Client{
			Transport: giteaTransport(p.GetGitea().GetAccessToken(), p.GetSkipVerify()),
		}
	case *pb.Provider_BitbucketCloud:
		client = bitbucket.NewDefault()
		client.Client = &http.Client{
			Transport: bitbucketTransport(p.GetBitbucketCloud().GetUsername(), p.GetBitbucketCloud().GetAppPassword(), p.GetSkipVerify()),
		}
	case *pb.Provider_BitbucketServer:
		if p.GetEndpoint() == "" {
			log.Error("getGitClient failure Bitbucket Server, endpoint is empty")
			return nil, status.Errorf(codes.InvalidArgument, fmt.Sprintf("Must provide an endpoint for %s", p.String()))
		}
		client, err = stash.New(p.GetEndpoint())
		if err != nil {
			log.Errorw("GetGitClient failure Bitbucket Server", "endpoint", p.GetEndpoint(), zap.Error(err))
			return nil, err
		}
		client.Client = &http.Client{
			Transport: bitbucketTransport(p.GetBitbucketServer().GetUsername(), p.GetBitbucketServer().GetPersonalAccessToken(), p.GetSkipVerify()),
		}
	default:
		log.Errorw("GetGitClient unsupported git provider", "endpoint", p.GetEndpoint())
		return nil, status.Errorf(codes.InvalidArgument, "Unsupported git provider")
	}
	if p.Debug {
		client.DumpResponse = func(resp *http.Response, body bool) ([]byte, error) {
			out, err := httputil.DumpResponse(resp, body)
			if err != nil {
				log.Errorw("GetGitClient debug dump failed", "endpoint", p.GetEndpoint())
			}
			log.Infow("GetGitClient debug", "dump", string(out))
			return nil, nil
		}
	}
	return client, nil
}

// returns a string of the git provider being used. Currently only its name.
func GetProvider(p pb.Provider) string {
	switch p.GetHook().(type) {
	case *pb.Provider_Github:
		return "github"
	case *pb.Provider_Gitlab:
		return "gitlab"
	case *pb.Provider_Gitea:
		return "gitea"
	case *pb.Provider_BitbucketCloud:
		return "bitbucket cloud"
	case *pb.Provider_BitbucketServer:
		return "bitbucket server"
	default:
		return "unknown provider"
	}
}
