// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package csharp

import (
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	mexec "github.com/harness/harness-core/commons/go/lib/exec"
	"github.com/harness/harness-core/commons/go/lib/filesystem"
	"github.com/harness/harness-core/commons/go/lib/logs"
	"github.com/harness/harness-core/product/ci/ti-service/types"
	"go.uber.org/zap"
	"testing"
)

func TestDotNet_GetCmd(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)

	runner := NewDotnetRunner(log.Sugar(), fs, cmdFactory)

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tests := []struct {
		name                 string // description of test
		args                 string
		runOnlySelectedTests bool
		want                 string
		expectedErr          bool
		tests                []types.RunnableTest
	}{
		{
			name:                 "run all tests with non-empty test list and runOnlySelectedTests as false",
			args:                 "test Build.csproj --test-adapter-path:. --logger:trx",
			runOnlySelectedTests: false,
			want:                 "dotnet test Build.csproj --test-adapter-path:. --logger:trx",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run all tests with empty test list and runOnlySelectedTests as false",
			args:                 "test Build.csproj --test-adapter-path:. --logger:trx",
			runOnlySelectedTests: false,
			want:                 "dotnet test Build.csproj --test-adapter-path:. --logger:trx",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with given test list",
			args:                 "test Build.csproj --test-adapter-path:. --logger:trx",
			runOnlySelectedTests: true,
			want:                 "dotnet test Build.csproj --test-adapter-path:. --logger:trx --filter \"FullyQualifiedName~pkg1.cls1|FullyQualifiedName~pkg2.cls2\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run selected tests with zero tests",
			args:                 "test Build.csproj",
			runOnlySelectedTests: true,
			want:                 "echo \"Skipping test run, received no tests to execute\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with repeating test list",
			args:                 "test Build.csproj",
			runOnlySelectedTests: true,
			want:                 "dotnet test Build.csproj --filter \"FullyQualifiedName~pkg1.cls1|FullyQualifiedName~pkg2.cls2\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2, t1, t2},
		},
		{
			name:                 "run selected tests with single test",
			args:                 "test Build.csproj",
			runOnlySelectedTests: true,
			want:                 "dotnet test Build.csproj --filter \"FullyQualifiedName~pkg2.cls2\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{t2},
		},
	}

	for _, tc := range tests {
		got, err := runner.GetCmd(ctx, tc.tests, tc.args, "/test/tmp/config.ini", false, !tc.runOnlySelectedTests)
		if tc.expectedErr == (err == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		assert.Equal(t, got, tc.want)
	}
}

func TestGetDotnetCmd_Manual(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)

	runner := NewDotnetRunner(log.Sugar(), fs, cmdFactory)

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tests := []struct {
		name                 string // description of test
		args                 string
		runOnlySelectedTests bool
		want                 string
		expectedErr          bool
		tests                []types.RunnableTest
	}{
		{
			name:                 "run all tests with empty test list and runOnlySelectedTests as false",
			args:                 "test Build.csproj",
			runOnlySelectedTests: false,
			want:                 "dotnet test Build.csproj",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with a test list and runOnlySelectedTests as true",
			args:                 "test Build.csproj",
			runOnlySelectedTests: true,
			want:                 "dotnet test Build.csproj",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
	}

	for _, tc := range tests {
		got, err := runner.GetCmd(ctx, tc.tests, tc.args, "/test/tmp/config.ini", true, !tc.runOnlySelectedTests)
		if tc.expectedErr == (err == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		assert.Equal(t, got, tc.want)
	}
}
